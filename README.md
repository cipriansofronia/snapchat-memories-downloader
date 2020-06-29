# snapchat-memories-downloader

#### Download memories from snapchat with ease!

If you end up here than you know that in order to download all your memories you'll have to 
manually download each photo and video from snapchat servers.

This tool will download each media file and set the right creation date to it.
Just unzip your archive which you previously downloaded from snapchat, locate the 
`memories_history.json` file from `json` folder and pass it to **snapchat-memories-downloader** 
tool the way it's presented bellow! All your files will be located in the `snapchat-memories` 
folder in the same place where [snapchat-memories-downloader.jar](https://github.com/cipriansofronia/snapchat-memories-downloader/releases/download/v0.2.0/snapchat-memories-downloader-assembly-0.2.0.jar) was downloaded.

#### How to run it:
In order to run it, you'll need java installed on your machine!
```bash
➜ java -jar snapchat-memories-downloader.jar -f /absolute/path/to/your/memories_history.json
```
Available options:
```bash
➜ java -jar snapchat-memories-downloader.jar -h
snapchat-memories-downloader 0.2.0
Usage: snapchat-memories-downloader [options]
  -v, --version
  -h, --help               prints this usage text
  -f, --memories-file <value>
                           absolute path to your 'memories_history.json' file - required
  -o, --nr-of-operations <value>
                           number of downloads to run concurrently - optional
  -ln, --last-nr-of-memories <value>
                           last N memories to download - optional
  -fn, --first-nr-of-memories <value>
                           first N memories to download - optional
  -ad, --memories-after-date <value>
                           memories filtered after a certain date, format yyyy-MM-dd - optional
  -bd, --memories-before-date <value>
                           memories filtered before a certain date, format yyyy-MM-dd - optional
```

#### How to build it:
You can always build the artifact yourself using this command: 
```bash
➜ sbt assembly
```
