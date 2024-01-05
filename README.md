# Marble Blast Launcher
A cross-platform client that automatically downloads, updates, and manages Marble Blast installations.

## Configuration
All configuration files use the JSON format, and follow a similar format:

For non-OS specific properties, a simple JSON key/value is used:  
`"key" : "value",`

OS-Specific properties use a sub-list with different values for each OS:
```
"key": {
    "mac"     : "Mac only, 10.7+",
    "windows" : "Windows only, XP+",
    "other"   : "Other OS (Linux or something)",
    "macold"  : "Mac only, 10.5 and 10.6"
},
```
With that, you can control which OS(es) get which package and file listings, and even distribute different versions for different OSes.
This format applies to all properties in configuration files, client or mod-specific.

### Main Client Config
The main client configuration should contain the following properties. Anything else will be ignored.

* `selfupdate` - Whether or not to automatically update the launcher
* `launchermd5` - The MD5 hash of the current launcher executable / JAR file
* `launcher` - The URL of the new launcher executable / JAR to download if an update is available.

### Mod-Specific Config
Each mod should provide one config.json file that will be used for determining how the launcher will treat the mod.
Separate files should be provided for package listing, file listing, migrations, searches, and deletions. They will be covered below.

The mod-specfic config makes use of the following properties:

#### Basic Parameters
* `name` - The identifier "name" of the mod, should be short, one word, and no spaces.
* `gamename` - The full name of the mod, which will be displayed to the user.
* `image` - A URL to an image that will be downloaded and displayed in the launcher to represent the mod.
* `title` - The title of the launcher window that will be displayed for your mod.
* `launchpath` - The path to the executable to launch for the mod.
* `opensub` - A subdirectory of your mod's install directory to open with the "Open Game Dir" button.

#### Launcher Pages
* `news` - A URL for the "News" page on the launcher.
* `changelog` - A URL for the "Changelog" page on the launcher.

#### External Listing Configuration Files
* `packages` - A URL to a package listing config file. See specification below.
* `listing` - A URL to a file listing config file. See specification below.
* `prunelist` - A URL to a deletion listing config file. See specification below.
* `conversions` - A URL to a conversion listing config file. See specification below.
* `migrations` - A URL to a migration listing config file. See specification below.
* `searches` - A URL to a search listing config file. See specification below.

#### Offline Mode Message
* `offlinetitle` - The title of the message that is displayed if the user is offline.
* `offlinemessage` - The body of the message to that is displayed if the user is offline.

#### Automatic Preference Copying
* `prefsfile` - The mod's current prefs.cs file that is read.
* `docopyprefs` - Whether or not the launcher should attempt to copy prefs from an old install.
* `copyprefsask` - Whether or not to ask the user first before attempting to copy prefs.
* `copyprefstitle` - The title of the message displayed to the user asking to copy prefs.
* `copyprefsmessage` - The body of the message displayed to the user asking to copy prefs.
* `copydata` - A list of files to copy prefs from, from the mod base directory.
* `lineending` - The line ending to be used for prefs files.

#### Crash Console Posting
* `doconsolepost` - Whether or not to ask users to send their console when a crash is detected.
* `consoleposttitle` - The title of the message that will be displayed to the user, asking them to send their console.
* `consolepostmessage` - The body of the message that will be displayed to the user, asking them to send their console.
* `consolepost` - A URL to a script that will receive the file uploaded via the console post system.
* `consolepostattachmentname` - The name of the attachment which is submitted in an HTTP POST request.
* `consolepostattachmentfile` - The file name for the attachment which is submitted.

### Package Listing
The package listing file should contain a basic JSON object with all potential package names mapped to their respective URLs.
The launcher will attempt to download the package from the given URL if it needs to be updated.

Example:
```
{
    "gui.zip"     : "http://files.marbleblast.com/1.51/gui.zip",
    "scripts.zip" : "http://files.marbleblast.com/1.51/scripts.zip",
    ...
}
```

### File Listing
The file listing contains a listing of every file in your mod, with its MD5 hash and containing package.
The launcher will check every file against this list, and determine updates based on which files have non-matching hashes.

If any item in the list does not contain a package or md5, the launcher will treat it as a directory.
Empty directories can be specified with an empty list.

If a file is not in this list, but in a user's game, it will be ignored.
Because of this, if you delete any files in patches to your mod, you will need to add the deleted files to the deletion listing.

Note: This file will generally be very large. It is easiest to use a script to generate this file.

Example:
```
{
    "main.cs" : {
        package : "scripts",
        "md5" : "09ed77ac07b2a07cbaad885642e43802"
    },
    "platinum" : {
        "main.cs.dso" : {
            "package" : "scripts",
            "md5": "09ed77ac07b2a07cbaad885642e43802"
        },
        "empty" : [],
        ...
    }
}
```
### Deletion Listing
The deletion listing contains all the files to delete from users' clients.

It is structured in a manner similar to the file listing, except, instead of objects as the values, empty strings are used.
Subdirectories are still denoted with a sub-object.

Example:
```
{
    "extra-file.txt" : "",
    "platinum" : {
		"confidential-information.cs.dso" : "",
		"data" : {
		    "removed-in-the-next-patch.cs.dso" : "",
		},
		...
	},
}
```
### Search Listing
The search listing contains a list of expressions (PCRE-compliant) to search for when copying prefs.
Only lines which contain a search term will be affected by the prefs copying system, using the conversions as specified below.
Lines which do not contain a search term will be copied unchanged.

Search terms are specified by the term corresponding to a blank string.

Example:
```
{
    "highscores" : "",
    "LevelTime" : "",
    ...
}
```
### Conversion Listing
The conversion listing is used when copying prefs.
It is composed of a list of search expresions (PCRE-compliant) and their equivalent replacements.

When preferences are copied, on lines which match any of the search items (see above), the launcher will search for all occurances of every item in this list, and replace them if they are found.

Example:
```
{
    "missions_mbg/advanced/selection\\.mis" : "missions_mbg/advanced/NaturalSelection.mis",
    "missions_mbg/advanced/construction\\.mis" : "missions_mbg/advanced/UnderConstruction.mis",

    "highscoresplatinumbeta" : "highscoresplatinum",
    "easterEggplatinumbeta" : "easterEggplatinum",
    
    "(?<!\\\\)\\'" : "\\\\'",
    ...
}
```
### Migration Listing
The migration listing is a list of directories to migrate within your mod.

Migration of a directory will rename it without needing to redownload the entire mod again.
This is used primarily if you change the base directory name of your mod (e.g. "platinumbeta" to "platinum").

Example:
```
{
    "platinumbeta" : "platinum",
    "common" : "platinum/core",
    ...
}
```
