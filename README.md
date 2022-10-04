# [Horizen](https://horizen.io/) Desktop GUI Wallet

<p align="center"><img src="https://www.horizen.io/assets/img/icons/page_media/logo_no_tagline.svg" width="600"></p>

## Deprecation notice

Horizen Desktop GUI Wallet will not receive any new features, future releases will be limited to critical bug fixes and compatibility with newer versions of zend.
[Sphere by Horizen](https://github.com/HorizenOfficial/Sphere_by_Horizen) is its successor and ongoing development will be focused on Sphere by Horizen, to migrate to Sphere by Horizen please see our [wiki]( https://horizenofficial.atlassian.net/wiki/spaces/ZEN/pages/729776153).

**Running Horizen Desktop GUI Wallet on macOS Catalina**: Horizen Desktop GUI Wallet is not going to be notarized and as such won't start on macOS Catalina without going through some extra steps, if you want to run it on MacOS Catalina or later please read and follow https://support.apple.com/en-us/HT202491 `How to open an app that hasn’t been notarized or is from an unidentified developer`.

## Graphical user interface wrapper for the [Horizen](https://horizen.io/) command line tools

This program provides a Graphical User Interface (GUI) for the Horizen client tools that acts as a wrapper and 
presents the information in a user-friendly manner.

![Screenshot](https://github.com/HorizenOfficial/zencash-swing-wallet-ui/raw/master/docs/ZENCashWallet.png "Main Window")

**This wallet is targeted at advanced users who understand the implications of running a full Zen node on**
**the local machine, maintaining a full local copy of the blockchain, maintaining and backing up the**
**Zen nodes's `wallet.dat` file etc! The wallet is not suitable for novice crypto-currency users!**

**SECURITY WARNING: Encryption of the wallet.dat file is not yet supported for Horizen. Using the wallet** 
**on a system infected with malware may result in wallet data/funds being stolen. The**
**wallet.dat needs to be backed up regularly (not just once - e.g. after every 30-40**
**outgoing transactions) and it must also be backed up after creating a new Z address.**

**STABILITY WARNING: The GUI wallet is as yet considered experimental! It is known to exhibit occasional stability problems related to running a full Zen node.**
**Specifically if the locally running `zend` cannot start properly due to issues with the local blockchain, the GUI cannot start either!**
**Users need to be prepared to fix such problems manually as described in the [troubleshooting guide](docs/TroubleshootingGuide.md).**
**Doing so requires command line skills.**

**AUTO-DEPRECATION WARNING: Wallet binary releases for Mac/Windows contain ZEN full node binaries. These have an auto-deprecation feature:**
**they are considered outdated after 16 weeks and stop working. So they need to be updated to a newer version before this term expires.**
**Users need to ensure they use an up-to-date version of the wallet (e.g. update the wallet every two months or so).**

#### New/Experimental: [Horizen Desktop GUI Wallet packages for Debian/Ubuntu Linux](https://github.com/HorizenOfficial/zencash-swing-wallet-ui/blob/master/docs/ReleaseUbuntuRepository.md) are available

#### New/Experimental: [Horizen Desktop GUI Wallet for Windows/macOS](https://github.com/HorizenOfficial/zencash-swing-wallet-ui/blob/master/docs/Release_1.0.7.md) is available

#### Information on diagnosing some common problems may be found in this [troubleshooting guide](docs/TroubleshootingGuide.md).

## Building, installing and running the Wallet GUI

Before installing the Desktop GUI Wallet you need to have Horizen up and running. The following 
[guide](https://github.com/HorizenOfficial/zen/blob/master/README.md) 
explains how to set up [Horizen](https://horizen.io/). 

**For security reasons it is recommended to always build the GUI wallet program from GitHub**
**[source](https://github.com/HorizenOfficial/zencash-swing-wallet-ui/archive/master.zip).**
The details of how to build it are described below (easy to follow). 


1. Operating system and tools

   As of January 2019 (Horizen v2.0.16) this program supports Linux, macOS Sierra/High Sierra and Windows.
   The Linux tools you need to build and run the Wallet GUI are Git, Java (JDK12) and Ant.
   To install OpenJDK to meet the Java dependency, please follow the instructions on these links:
   [Windows](https://adoptopenjdk.net/installation.html?variant=openjdk12&jvmVariant=hotspot#windows-msi)
   [Linux](https://adoptopenjdk.net/installation.html?variant=openjdk12&jvmVariant=hotspot#linux-pkg)
   [MacOS](https://adoptopenjdk.net/installation.html?variant=openjdk12&jvmVariant=hotspot#macos-pkg)

   For RedHat/CentOS/Fedora-type, you should be able to install git and ant running the following command:
   ```
   user@centos:~/build-dir$ sudo yum install git ant 
   ```
   If you have some Linux distribution that those instructions do not apply to, please check your relevant documentation on installing Git, 
   JDK and Ant. The commands `git`, `java`, `javac` and `ant` need to be startable from command line 
   before proceeding with build.

2. Building from source code

   As a start you need to clone the zencash-swing-wallet-ui Git repository:
   ```
   user@ubuntu:~/build-dir$ git clone https://github.com/HorizenOfficial/zencash-swing-wallet-ui.git
   ```
   Change the current directory:
   ```
   user@ubuntu:~/build-dir$ cd zencash-swing-wallet-ui/
   ```
   Issue the build command:
   ```
   user@ubuntu:~/build-dir/zencash-swing-wallet-ui$ ant -buildfile ./src/build/build.xml
   ```
   This takes a few seconds and when it finishes, it builds a JAR file `./build/jars/ZENCashSwingWalletUI.jar`. 
   You need to make this file executable:
   ```
   user@ubuntu:~/build-dir/zencash-swing-wallet-ui$ chmod u+x ./build/jars/ZENCashSwingWalletUI.jar
   ```
   At this point the build process is finished the built GUI wallet program is the JAR 
   file `./build/jars/ZENCashSwingWalletUI.jar`. In addition the JAR file 
   `bitcoinj-core-0.14.5.jar` is also necessary to run the wallet. 

3. Installing the built Horizen GUI wallet

   3.1. If you have built Horizen from source code:

     Assuming you have already built from source code [Horizen](https://github.com/HorizenOfficial/zen) in directory `/home/user/zen/src` (for example - this is the typical build dir. for Horizen v2.0.16) which contains the command line tools `zen-cli` and `zend` you need to take the created JAR files and copy them to directory `/home/user/zen/src` (the same dir. that contains `zen-cli` and `zend`). Example copy command:
      ```
      user@ubuntu:~/build-dir/zencash-swing-wallet-ui$ cp -R -v ./build/jars/* /home/user/zen/src    
      ```

4. Running the installed Horizen GUI wallet

   It may be run from command line or started from another GUI tool (e.g. file manager). 
   Assuming you have already installed [Horizen](https://horizen.io/) and the GUI Wallet `ZENCashSwingWalletUI.jar` in 
   directory `/home/user/zen/src` one way to run it from command line is:
   ```
   user@ubuntu:~/build-dir/zencash-swing-wallet-ui$ java -jar /home/user/zen/src/ZENCashSwingWalletUI.jar
   ```
   If you are using Ubuntu (or similar ;) Linux you may instead just use the file manager and 
   right-click on the `ZENCashSwingWalletUI.jar` file and choose the option "Open with OpenJDK 8 Runtime". 
   This will start the Horizen GUI wallet.
   
   **Important:** the Horizen configuration file `~/.zen/zen.conf` needs to be correctly set up for the GUI
   wallet to work. Specifically the RPC user and password need to be set in it like:
   ```
   rpcuser=username
   rpcpassword=wjQOHVDQFLwztWp1Ehs09q7gdjHAXjd4E
    
   ``` 


### License
This program is distributed under an [MIT License](https://github.com/HorizenOfficial/zencash-swing-wallet-ui/raw/master/LICENSE).

### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

### Known issues and limitations

1. Issue: The Horizen Desktop GUI Wallet is not compatible with applications that modify the ZEN `wallet.dat` file. The wallet should not be used
with such applications on the same PC. For instance some distributed exchange applications are known to create watch-only addresses in the
`wallet.dat` file that cause the GUI wallet to display a wrong balance and/or display addresses that do not belong to the wallet. 
1. Limitation: if two users exchange text messages via the messaging UI TAB and one of them has a system clock, substantially running slow or fast by more than 1 minute, it is possible that this user will see text messages appearing out of order. 
1. Limitation: if a messaging identity has been created (happens on first click on the messaging UI tab), then replacing the `wallet.dat` or changing the node configuration between mainnet and testnet will make the identity invalid. This will result in a wallet update error. To remove the error the directory `~/.ZENCashSwingWalletUI/messaging` may be manually renamed or deleted (when the wallet is stopped). **CAUTION: all messaging history will be lost in this case!**
1. Limitation: Wallet encryption has been temporarily disabled in Horizen due to stability problems. A corresponding issue 
[#1552](https://github.com/zcash/zcash/issues/1552) has been opened by the ZCash developers. Correspondingly
wallet encryption has been temporarily disabled in the Horizen Desktop GUI Wallet.
The latter needs to be disabled. 
1. Limitation: The list of transactions does not show all outgoing ones (specifically outgoing Z address 
transactions). A corresponding issue [#1438](https://github.com/zcash/zcash/issues/1438) has been opened 
for the ZCash developers. 
1. Limitation: The CPU percentage shown to be taken by zend on Linux is the average for the entire lifetime 
of the process. This is not very useful. This will be improved in future versions.
1. Limitation: When using a natively compiled wallet version (e.g. `ZENCashSwingWalletUI.exe` for Windows) on a 
very high resolution monitor with a specifically configured DPI scaling (enlargement) factor to make GUI 
elements look larger, the GUI elements of the wallet actually do not scale as expected. To correct this on
Windows you need to right-click on `ZENCashSwingWalletUI.exe` and choose option:
```
Properties >> Compatibility >> Override High DPI scaling behavior >> Scaling Performed by (Application)
```
