package me.nikl.gamebox.games.gemcrush;

import me.nikl.gamebox.GameBox;
import me.nikl.gamebox.games.Game;
import me.nikl.gamebox.games.GameSettings;
import me.nikl.gamebox.games.GemCrushMain;
import me.nikl.gamebox.games.gemcrush.game.GameManager;

public class GemCrush extends Game {
	private final String[][] depends =  new String[][]{
			new String[]{"Vault", "1.5"},
			new String[]{"GameBox", "1.5.0"}
	};
	private final String[] subCommands = new String[]{
			"gemcrush", "gc"
	};

	public GemCrush(GameBox gameBox) {
		super(gameBox, GemCrushMain.GEM_CRUSH);
	}

	@Override
	public void onDisable() {

	}

	@Override
	public void init() {

	}

	@Override
	public void loadSettings() {
		gameSettings.setGameType(GameSettings.GameType.SINGLE_PLAYER);
		gameSettings.setGameGuiSize(54);
		gameSettings.setHandleClicksOnHotbar(false);
	}

	@Override
	public void loadLanguage() {
		gameLang = new Language(this);
	}

	@Override
	public void loadGameManager() {
		gameManager = new GameManager(this);
	}

	/*
	private void hook() {
		if (Bukkit.getPluginManager().getPlugin("GameBox") == null || !Bukkit.getPluginManager().getPlugin("GameBox").isEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes( '&', " &cGameBox not found!"));
			Bukkit.getLogger().log(Level.SEVERE, "   Get the newest version here:");
			Bukkit.getLogger().log(Level.SEVERE, "   https://www.spigotmc.org/resources/37273/");
			Bukkit.getPluginManager().disablePlugin(this);
			disabled = true;
			return;
		}



		gameBox = (me.nikl.gamebox.GameBox)Bukkit.getPluginManager().getPlugin("GameBox");


		String[] versionString = gameBox.getDescription().getVersion().replaceAll("[^0-9.]", "").split("\\.");
		String[] minVersionString = depends[1][1].split("\\.");
		Integer[] version = new Integer[versionString.length];
		Integer[] minVersion = new Integer[minVersionString.length];

		for(int i = 0; i < minVersionString.length; i++){
			try {
				minVersion[i] = Integer.valueOf(minVersionString[i]);
				version[i] = Integer.valueOf(versionString[i]);
			} catch (NumberFormatException exception){
				exception.printStackTrace();
			}
		}

		for(int i = 0; i < minVersion.length; i++){
			if(minVersion[i] < version[i]) break;
			if(minVersion[i].equals(version[i])) continue;

			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes( '&', " &cYour GameBox is outdated!"));
			Bukkit.getLogger().log(Level.WARNING, " Get the latest version here: ");
			Bukkit.getLogger().log(Level.WARNING, "   https://www.spigotmc.org/resources/37273/");
			Bukkit.getLogger().log(Level.WARNING, " You need at least version " + depends[1][1]);
			Bukkit.getPluginManager().disablePlugin(this);
			disabled = true;
			return;
		}


		// disable economy if it is disabled for either one of the plugins
		this.econEnabled = this.econEnabled && GameBoxSettings.econEnabled;
		playSounds = playSounds && GameBoxSettings.playSounds;

		GUIManager guiManager = gameBox.getPluginManager().getGuiManager();

		this.manager = new GameManager(this);

		gameBox.getPluginManager().registerGame(this, manager, gameID, me.nikl.gamebox.games.gemcrush.Language.name, 1);


		int gameGuiSlots = 54;
		GameGui gameGui = new GameGui(gameBox, guiManager, gameGuiSlots, gameID, GUIManager.MAIN_GAME_GUI);
		gameGui.setHelpButton(lang.GAME_HELP);



		Map<String, GameRules> gameTypes = new HashMap<>();

		if(config.isConfigurationSection("gameBox.gameButtons")){
			ConfigurationSection gameButtons = config.getConfigurationSection("gameBox.gameButtons");
			ConfigurationSection buttonSec;
			int moves, numberOfGems;
			double cost;
			boolean bombs, saveStats;

			String displayName;
			ArrayList<String> lore;

			GameRules rules;

			for(String buttonID : gameButtons.getKeys(false)){
				buttonSec = gameButtons.getConfigurationSection(buttonID);


				if(!buttonSec.isString("materialData")){
					Bukkit.getLogger().log(Level.WARNING, " missing material data under: gameBox.gameButtons." + buttonID + "        can not load the button");
					continue;
				}

				ItemStack mat = ItemStackUtil.getItemStack(buttonSec.getString("materialData"));
				if(mat == null){
					Bukkit.getLogger().log(Level.WARNING, " error loading: gameBox.gameButtons." + buttonID);
					Bukkit.getLogger().log(Level.WARNING, "     invalid material data");
					continue;
				}


				AButton button =  new AButton(mat.getData(), 1);
				ItemMeta meta = button.getItemMeta();

				if(buttonSec.isString("displayName")){
					displayName = GameBox.chatColor(buttonSec.getString("displayName"));
					meta.setDisplayName(displayName);
				}

				if(buttonSec.isList("lore")){
					lore = new ArrayList<>(buttonSec.getStringList("lore"));
					for(int i = 0; i < lore.size();i++){
						lore.set(i, GameBox.chatColor(lore.get(i)));
					}
					meta.setLore(lore);
				}

				button.setItemMeta(meta);
				button.setAction(ClickAction.START_GAME);
				button.setArgs(gameID, buttonID);

				bombs = buttonSec.getBoolean("bombs", true);
				moves = buttonSec.getInt("moves", 20);
				numberOfGems = buttonSec.getInt("differentGems", 8);
				cost = buttonSec.getDouble("cost", 0.);
				saveStats = buttonSec.getBoolean("saveStats", false);


				rules = new GameRules(this, moves, numberOfGems, bombs, cost, saveStats, buttonID);

				setTheButton:
				if(buttonSec.isInt("slot")){
					int slot = buttonSec.getInt("slot");
					if(slot < 0 || slot >= gameGuiSlots){
						Bukkit.getLogger().log(Level.WARNING, "the slot of gameBox.gameButtons." + buttonID + " is out of the inventory range (0 - "+ gameGuiSlots +")");
						gameGui.setButton(button);
						break setTheButton;
					}
					gameGui.setButton(button, slot);
				} else {
					gameGui.setButton(button);
				}

				gameTypes.put(buttonID, rules);
			}
		}


		this.manager.setGameTypes(gameTypes);



		getMainButton:
		if(config.isConfigurationSection("gameBox.mainButton")){
			ConfigurationSection mainButtonSec = config.getConfigurationSection("gameBox.mainButton");
			if(!mainButtonSec.isString("materialData")) break getMainButton;

			ItemStack gameButton = ItemStackUtil.getItemStack(mainButtonSec.getString("materialData"));
			if(gameButton == null){
				gameButton = (new ItemStack(Material.EMERALD));
			}
			ItemMeta meta = gameButton.getItemMeta();
			meta.setDisplayName(GameBox.chatColor(mainButtonSec.getString("displayName","&3GemCrush")));
			if(mainButtonSec.isList("lore")){
				ArrayList<String> lore = new ArrayList<>(mainButtonSec.getStringList("lore"));
				for(int i = 0; i < lore.size();i++){
					lore.set(i, GameBox.chatColor(lore.get(i)));
				}
				meta.setLore(lore);
			}
			gameButton.setItemMeta(meta);
			guiManager.registerMainGameGUI(gameGui, gameButton, subCommands);
		} else {
			Bukkit.getLogger().log(Level.WARNING, " Missing or wrong configured main button in the configuration file!");
		}


		// get top list buttons
		if(config.isConfigurationSection("gameBox.topListButtons")){
			ConfigurationSection topListButtons = config.getConfigurationSection("gameBox.topListButtons");
			ConfigurationSection buttonSec;

			ArrayList<String> lore;


			for(String buttonID : topListButtons.getKeys(false)){
				buttonSec = topListButtons.getConfigurationSection(buttonID);

				if(!gameTypes.keySet().contains(buttonID)){
					Bukkit.getLogger().log(Level.WARNING, " the top list button 'gameBox.topListButtons." + buttonID + "' does not have a corresponding game button");
					continue;
				}


				if(!gameTypes.get(buttonID).isSaveStats()){
					Bukkit.getLogger().log(Level.WARNING, " the top list buttons 'gameBox.topListButtons." + buttonID + "' corresponding game button has statistics turned off!");
					Bukkit.getLogger().log(Level.WARNING, " With these settings there is no toplist to display");
					continue;
				}

				if(!buttonSec.isString("materialData")){
					Bukkit.getLogger().log(Level.WARNING, " missing material data under: gameBox.topListButtons." + buttonID + "        can not load the button");
					continue;
				}

				ItemStack mat = ItemStackUtil.getItemStack(buttonSec.getString("materialData"));
				if(mat == null){
					Bukkit.getLogger().log(Level.WARNING, " error loading: gameBox.topListButtons." + buttonID);
					Bukkit.getLogger().log(Level.WARNING, "     invalid material data");
					continue;
				}


				AButton button =  new AButton(mat.getData(), 1);
				ItemMeta meta = button.getItemMeta();

				if(buttonSec.isString("displayName")){
					meta.setDisplayName(GameBox.chatColor(buttonSec.getString("displayName")));
				}


				if(buttonSec.isList("lore")){
					lore = new ArrayList<>(buttonSec.getStringList("lore"));
					for(int i = 0; i < lore.size();i++){
						lore.set(i, GameBox.chatColor(lore.get(i)));
					}
					meta.setLore(lore);
				}

				button.setItemMeta(meta);
				button.setAction(ClickAction.SHOW_TOP_LIST);
				button.setArgs(gameID, buttonID + GUIManager.TOP_LIST_KEY_ADDON);



				setTheButton:
				if(buttonSec.isInt("slot")){
					int slot = buttonSec.getInt("slot");
					if(slot < 0 || slot >= gameGuiSlots){
						Bukkit.getLogger().log(Level.WARNING, "the slot of gameBox.topListButtons." + buttonID + " is out of the inventory range (0 - "+ gameGuiSlots +")");
						gameGui.setButton(button);
						break setTheButton;
					}
					gameGui.setButton(button, slot);
				} else {
					gameGui.setButton(button);
				}

				// get skull lore and pass on to the top list page
				if(buttonSec.isList("skullLore")){
					lore = new ArrayList<>(buttonSec.getStringList("skullLore"));
					for(int i = 0; i < lore.size();i++){
						lore.set(i, GameBox.chatColor(lore.get(i)));
					}
				} else {
					lore = new ArrayList<>(Arrays.asList("", "No lore specified in the config!"));
				}

				TopListPage topListPage = new TopListPage(gameBox, guiManager, 54, gameID,
						buttonID + GUIManager.TOP_LIST_KEY_ADDON,
						GameBox.chatColor(buttonSec.getString("inventoryTitle", "Title missing in config")),
						SaveType.SCORE, lore);

				guiManager.registerGameGUI(topListPage);
			}
		}
	}
	
	@Override
	public void onDisable(){

	}

	
	public NMSUtil getUpdater(){
		return this.updater;
	}
	
    private boolean setupEconomy(){
    	if (getServer().getPluginManager().getPlugin("Vault") == null) {
    		return false;
    	}
    	RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    	if (rsp == null) {
    		return false;
    	}
    	econ = rsp.getProvider();
    	return econ != null;
    }
	
	public void reloadConfig(){
		try { 
			this.config = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(this.con), "UTF-8")); 
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
			disabled = true;
		}
	} 
	
	public GameManager getManager() {
		return manager;
	}
	
	public void reload(){
		//shut down all games on reload
		if(manager != null){
			manager.shutDown();
		}
		if(!con.exists()){
			this.saveResource("config.yml", false);
		}
		reloadConfig();
		
		if(config.isBoolean("playSounds"))
			playSounds = config.getBoolean("playSounds");
		
		
		this.lang = new me.nikl.gamebox.games.gemcrush.Language(this);
		
		this.econEnabled = false;
		if(config.getBoolean("economy.enabled")){
			this.econEnabled = true;
			if(!setupEconomy()){
				Bukkit.getLogger().log(Level.WARNING, " Failed to set up economy...");
				Bukkit.getLogger().log(Level.WARNING, " Do you have vault and an economy plugin installed?");
				this.econEnabled = false;
				disabled = true;
				return;
			}
		}
	}

	public FileConfiguration getConfig() {
		return config;
	}
    
    public Boolean getEconEnabled(){
    	return this.econEnabled;
    }
    */
}

