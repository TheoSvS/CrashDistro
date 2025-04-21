A program that monitors the CrashOut game on Solana in order to extract some valuable statistics to increase bet gains and win rate. 

It monitors the historical data of the past games and provides insight on best betting strategies at any given time, since the game results of this game don't seem to be entirely and properly randomized.. 

In example, with a starting bet of $50 the portfolio value has reached $2000 in around 10-12 days of consistent strategical betting.

I will not provide my betting strategy. The user is supposed to add his own betting conditions by implementing DecisionMaker interface and overriding the default implementation, based on his observations and strategy.

The application needs the funding wallet's private key in order to sign the betting and claiming transactions. You should edit the solcrash.properties file at the project root directory and add to the property "secret = " the private key of the wallet.

In AppConstants file, you should modify the betCmd and claimCmd AccountMetas lists to match those of your own betting account. To find them, make a normal bet in game with your browser. Then go to https://solscan.io/, locate your wallet, and the locate the bet transaction you just did.
Under "Transaction Details" of that transaction, locate #3 instruction which is the bet instruction. Use the AccountMetas found there, in that order to replace the betCmd AccountMetas in AppConstants. There should be only 8 accounts. Most would be the same, except your own wallet, and your ingame pending balance wallet. Do not change any other account properties apart from the public addresses when replacing the AccountMetas in AppConstants. 
Finally do a "normal" Claim through the game's website for some funds you have won and are pending. Do the same process to locate this transaction in solscan, and do the same process to copy the AccountMetas of this transaction to modify the claimCmd AccountMetas in AppConstants. There should be only 3 accounts in that step. Do not change any other account properties apart from the public addresses when replacing the AccountMetas in AppConstants. 
You should now be ready to place automatic bets.

DISCLAIMER:
This source code and software is intended solely for experimentation and educational purposes. It is provided on an “as is” basis, without warranties or conditions of any kind. By using this application, you acknowledge and agree that the author(s), contributor(s), and distributor(s) shall not be held liable for any financial losses, damages, or other harm (direct or indirect) that may result from its use. You assume full responsibility for any decisions or actions taken based on the information provided by this application. Always conduct your own research and seek professional advice before making any financial or investment decisions.
