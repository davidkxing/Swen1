# MTCG Project
This HTTP/REST-based server is built to be a platform for trading and battling with and
against each other in a magical card-game world. 

## Unit Test

### Server
#### requestInfoTest
	testConstructor()

### GameLogic
#### cardHandlerTest
	getCardsFailTest()
	getDeckFailTest()
	setDeckFailTestNotEnoughCards()
	setDeckFailTestNotYourCards()

#### shopHandlerTest
	buyPackageFailTest()
	tradeFailTestNotExisting()
	tradeFailTestTradeYourself()
	tradeFailTestNotYourCard()
	tradeFailTestRequirementsNotMet()
	getTradesFailTest()

#### userHandlerTest
	createDatabase()
	createUserFailTest()
	loginUserSuccessTest()
	loginUserFailTest()
	getUserDataFailTest()
	setUserDataFailTest()
	getStatsFailTest()
	getScoreFailTest()
	getBattleLogsFailTestEmptyReply()
	getBattleLogsFailNoToken()

### Unique Feature
Save battle log in Folder battlelog

### Time Documentation
#### Test
	Unit Test: ~5h

#### Plan
	UML Diagram and Database: ~5h(mostly research)

#### Coding
	Server: ~14h
	Datahandling including database creation: ~22h
	Battlelogic: 35 - 50h

[MTCG on GitHub] -> https://github.com/davidkxing/Swen1