# snapchat-memories-downloader

#### Download memories from snapchat with ease!

If you end up here than you know that in order to download all your memories you'll have to 
manually download each photo and video from snapchat servers.

This tool will download each media file and set the right creation date to it.
Just unzip your archive which you previously downloaded from snapchat, locate the 
`memories_history.json` file from `json` folder and pass it to **snapchat-memories-downloader** 
tool the way it's presented bellow! All your files will be located in the `snapchat-memories` 
folder in the same place where [snapchat-memories-downloader.jar](https://github.com/cipriansofronia/snapchat-memories-downloader/releases/download/v0.1.1/snapchat-memories-downloader-assembly-0.1.2.jar) was downloaded.

#### How to run it:
In order to run it, you'll need java installed on your machine!
```bash
java -jar snapchat-memories-downloader.jar /absolute/path/to/your/memories_history.json
```

#### How to build it:
You can always build the artifact yourself using this command: 
```bash
sbt assembly
```