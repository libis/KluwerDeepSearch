# KluwerDeepSearch adaptor for PRIMO


1. compile KluwerDeepSearch.java 


```sh
scp KluwerDeepSearch.class primo@YOUR_PRIMO.hosted.exlibrisgroup.com:/exlibris/primo/p4_1/ng/primo/home/system/search/conf/thirdNodeImpl/com/exlibris/primo/thirdnode/thirdnodeimpl
```

## parameters
* mock              true/false
* mock_file         file name and path of mock file see data/kluwer.json
* log_to_file       file name and path of log file
* subscription      subscription key. Get 1 from the portal
* clientId          id(username) get it from Kluwer after registering the class
* clientSecret      secret(password) get it from Kluwer after registering the class