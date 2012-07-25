package com.solitude.slots.service;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.ThreadManager;
import com.solitude.slots.cache.CacheStoreException;
import com.solitude.slots.data.DataStoreException;
import com.solitude.slots.entities.Player;
import com.solitude.slots.entities.SpinResult;

/**
 * Create and manage slot machine behavior
 * @author kwright
 */
public class SlotMachineManager {
	
	/** singleton instance */
	private static final SlotMachineManager instance = new SlotMachineManager();
	/** @return singleton */
	public static SlotMachineManager getInstance() { return instance; }	
	/** logger */
    private static final Logger log = Logger.getLogger(instance.getClass().getName());
    
	/** array of spin results */
	private static final SpinResult[] spinResults = new SpinResult[10000];
	static {
		/** Load table set into map for very quick look up.  Properties file should be in the following format 
		 * where min/max are from 0 to 9999 with no overlap:
		 * table.result.coins.0=
		 * table.result.symbols.0=<comma separated ints indexed from 0, i.e. 0,0,0>
		 * table.result.range.min.0=0
		 * table.result.range.max.0=100
		 * */
		try {			
			Properties properties = new Properties();
			properties.load(new FileReader("WEB-INF/pay-out-table.properties"));
			String coinsStr = null;
			int index = 0;
			while ((coinsStr = properties.getProperty("table.result.coins."+index)) != null) {
				try {
					int coins = Integer.parseInt(coinsStr);
					String[] symbolsStrArray = ((String)properties.get("table.result.symbols."+index)).split(",");
					int[] symbols = new int[symbolsStrArray.length];
					for (int i=0;i<symbolsStrArray.length;i++) {
						symbols[i] = Integer.parseInt(symbolsStrArray[i]);
					}
					int minRange = Integer.parseInt(properties.getProperty("table.result.range.min."+index));
					int maxRange = Integer.parseInt(properties.getProperty("table.result.range.max."+index));
					SpinResult sprinResult = new SpinResult(coins,symbols);
					for (int i=minRange;i<=maxRange;i++) {
						if (spinResults[i] != null) {
							log.log(Level.WARNING,"range "+i+" already filled?!");
							continue;
						}
						spinResults[i] = sprinResult;
						
					}
					log.log(Level.INFO,"Loaded table "+index+" min="+minRange+", max="+maxRange+" symbols="+(String)properties.get("table.result.symbols."+index));
				} catch (Exception e) {
					log.log(Level.WARNING,"Unable to load pay out table index: "+index,e);
				}
				index++;
			}
		} catch (IOException e) {
			log.log(Level.SEVERE,"Unable to load pay out table!");
		}
	}
	
	/** random generator (nextInt is thread-safe) */
	private static final Random random = new Random();
	
	/** private constructor to ensure singleton */
	private SlotMachineManager() { }
	
	/**
	 * 
	 * @param player
	 * @param coins
	 * @return
	 * @throws InsufficientFundsException
	 * @throws CacheStoreException 
	 * @throws DataStoreException 
	 */
	public SpinResult spin(final Player player, int coins) throws InsufficientFundsException, DataStoreException, CacheStoreException {
		if (player.getCoins() < coins) throw new InsufficientFundsException();
		// spin!
		SpinResult spinResult = null;
		int attempts = 0;
		int idx=0;
		
		do {
			idx=random.nextInt(spinResults.length);
			spinResult = spinResults[idx];
		} while (spinResult==null && attempts++<10);

		// debit and credit player coins
		if (coins == 3 && spinResult.getCoins() > 0) {
			spinResult = new SpinResult(
					spinResult.getCoins()*Integer.parseInt(System.getProperty("max.bet.coin.multiplier")),
							spinResult.getSymbols());
		}
		player.setCoins(player.getCoins()-coins+spinResult.getCoins());
		player.setCoinsWon(spinResult.getCoins());
		// increment xp with # of coins spent and update leaderboard (do this with batching later?)
		player.setXp(player.getXp()+coins);
		PlayerManager.getInstance().storePlayer(player);
		if (Boolean.getBoolean("xp.leaderboard.enabled") && Boolean.getBoolean("xp.leaderboard.synchronous")) {
			try {
				ThreadManager.createBackgroundThread(new Runnable() {
					public void run() {
						try {
							OpenSocialService.getInstance().setScores(player.getMocoId(),
									new OpenSocialService.ScoreUpdate((short)1, player.getXp(), false),
									new OpenSocialService.ScoreUpdate((short)2, player.getCoinsWon(), false));
						} catch (Exception ex) {
							log.log(Level.WARNING,"error submitting score for player: "+player,ex);
							throw new RuntimeException(ex);
						}
					}
				}).start();
			} catch (Exception e) {
				log.log(Level.WARNING,"error creating lb thread for submitting score for player: "+player,e);
			}		
		}
		return spinResult;
	}

	
	/**
	 * Occurs when user does not have sufficient funds for spin
	 * @author kwright
	 */
	@SuppressWarnings("serial")
	public static class InsufficientFundsException extends Exception {}
}
