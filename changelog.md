# Change log


### v4.0.0-beta
- update to GAmeBox v3 and MC 1.14+

#

### v 3.0.1
- empty inventory when player closes the game to stop players from taking ghost items.

### v 3.0.0
- update to gamebox v2
- mavenized repository

# 

### v 2.3.1
- check for GB flag and if set: check the inventory title length

### v 2.3.0
- push GB dependency to 1.5.0
  - compatible with /gba reload

### v 2.2.0
- centralised more code to GameBox
  - use static main-key from GUIManager (GameBox) for guis
  - chat color in Main class
  - Sounds
  - use ItemStackUtil from GameBox to load ItemStacks
- removed deprecated methods and variables (now depends on GB version 1.3.0)
- remove all non numbers of the version string
- give link to GameBox when it is outdated
- add spanish lang file

### v 2.0.0
- depends on GameBox!
- no own starting command anymore
- highly configurable games (different settings/rules)
- no payment message when the cost is 0

### v 1.2.2:
- configurable sound volume in the configuration file
- changes in the structure of the top list apply after gc reload now
- improved matching algorithm
  - fixed wrong matching of different gem in between 2 triplets of the same gem
- fixed that sometimes the runnable of a game continued running after the inventory was closed. This did not cause any problems apart from unnecessary load.
- fixed matching of bombs

### v 1.1.0:
- implemented bombs
  - they will spawn glowing and explode later
  - added section in config: 'game.bombs' for customisation of bombs
    - without that section bombs are disabled be default!
- fixed glow bug for versions below 1.1.0
  - glow stayed after clicking if the clicked gem was switched but not destroyed
- added glow to all gems scheduled to break
- fixed glow for version 1.11
