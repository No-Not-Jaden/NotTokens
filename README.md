# NotTokens Overview
A simple tokens system for minecraft. Allows players with the proper permission to add or remove tokens from a player. Stores token amounts in the plugin while the server is running and auto saves to a file every couple of minutes. Also has a log of when and who gets tokens if there are any concerns about token balances.

# PlaceholderAPI
Not Tokens has a PlaceholderAPI extension: %nottokens_amount%

# Config

In the config, you can change the language as needed. (the messages your players see) Newer versions of the plugin (1.2<) allow you to reward the player tokens for killing mobs in kill-rewards. With this new feature, the token messages can be quite spammy when killing many mobs, so there is a condense-spam option. Set to the desired minimum message interval.

# Commands
/tokens help - displays commands
/tokens reload - reloads config
/tokens give (player) (amount) - gives a player tokens
/tokens remove (player) (amount) - removes a player's tokens
/tokens set (player) (amount) - removes a player's tokens
[Aliases: token, nottokens, ntokens]

# Permissions
nottokens.admin - ability to use all of the commands
