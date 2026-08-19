Is there a way to combine multiple versions into a single Plugins JAR?
So CarpetPlayers.jar could be used on both version 1.16.5 and 1.21.11 with the same features, even though the code is slightly different.
Also, 1.16.5 and 1.21.11 use the Via Plugins API (meaning Version and Backwards), so that if there is a player on the server that has Via Plugins.
Then that player's textures and calls will be determined by their Minecraft client. For example, if I'm on 1.16.5 and just joined a 1.21.11 server via Via Plugins.
Then CarpetPlayers Plugin would determine whether I still see the item textures, or whether they are backported to 1.16.5, or whether it uses code with the 1.16.5 system sent to my Minecraft client; later, if they use it, it will be sent to the server. The structure would be like this:
Download CarpetPlayers Plugin ---> 1.21.11 Server via Plugins ---> 1.16.5 Player joins ---> Code determines the code that the 1.16.5 player will use ---> Sent to the 1.16.5 player's Minecraft client ---> Sent in real-time to the server using the code that was provided.

I use Via Plugins on my 1.21.11 server.


Note: Via Plugins = ViaBackwards and ViaVersion.
But if "Via's" = all the plugins made by the Via Group such as Rewind, Version, Backwards, etc.

Oh, and also give the AI bot the ability to use /help so it can detect all commands and run those commands.
Also, the AI bot can use admin commands and custom admin plugin commands if it wants to.
But only if it has a role that is allowed to use those commands.

And also, it can use special items, like the items we use with the WorldEdit plugin/mod.
Then it could use the "//wand" command and select the location, then use commands like "//set", "//replace", etc.
Basically, it can look at the server's SRC (source code) and then use it like a normal player, but smarter for PVP, item usage, and so on.
And it could also help out (well, maybe it would just be a burden to us).

You can download the GitHub source code of each plugin you need, then copy the code you need for this project.

Oh, and this AI bot can also create its own ranks, complete with their colors.

Useful bot :)
