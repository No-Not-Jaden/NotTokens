# NotTokens
A simple tokens system for minecraft.
Allows players with the proper permission to add or remove tokens from a player.
Stores token amounts in a hashmap while the server is running and auto saves to a file every couple of minutes.
Also has a log of when and who gets tokens if there are any concerns about token balances.
In the config you can change the language as needed.
Includes a Placeholder from [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/), but not required. *%nottokens_amount%* 
| [Spigot Page](https://www.spigotmc.org/resources/nottokens.95946/)
You can reward players by giving them tokens for killing certain mobs set in the config. This can get a little spammy so there is a condense message option as well to only update the player on token changes after a set interval.
