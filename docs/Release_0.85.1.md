## [Horizen](https://horizen.global/) Desktop GUI Wallet binary release 0.85.1 for Mac OS Sierra/High Sierra/Mojave

This is a [Horizen](https://horizen.global/) Desktop GUI Wallet binary release 0.85.1 for Mac OS. 
It includes [Horizen 2.0.16 binaries](https://github.com/HorizenOfficial/zen/releases/tag/v2.0.16).

**This release does not support OS X 10.11 El Capitan anymore which is end of life,**
**to migrate to a different wallet follow this [GUIDE](https://horizenofficial.atlassian.net/wiki/x/X4AoC).**

**This wallet is targeted at advanced users who understand the implications of running a full Zen node on**
**the local machine, maintaining a full local copy of the blockchain, maintaining and backing up the**
**Zen nodes's `wallet.dat` file etc! The wallet is not suitable for novice crypto-currency users!**

**SECURITY WARNING: Encryption of the wallet.dat file is not yet supported for Horizen. Using the wallet** 
**on a system infected with malware may result in wallet data/funds being stolen. The**
**wallet.dat needs to be backed up regularly (not just once - e.g. after every 30-40**
**outgoing transactions) and it must also be backed up after creating a new Z address.**

**STABILITY WARNING: The GUI wallet is as yet considered experimental! It is known to exhibit occasional stability problems related to running a full Zen node.**
**Specifically if the locally running `zend` cannot start properly due to issues with the local blockchain, the GUI cannot start either!**
**Users need to be prepared to fix such problems manually as described in the [troubleshooting guide](TroubleshootingGuide.md).**
**Doing so requires command line skills.**

**AUTO-DEPRECATION WARNING: Wallet binary releases for Mac/Windows contain ZEN full node binaries. These have an auto-deprecation feature:**
**they are considered outdated after 16 weeks and stop working. So they need to be updated to a newer version before this term expires.**
**Users need to ensure they use an up-to-date version of the wallet (e.g. update the wallet every two months or so).**

### Installing the Horizen Desktop GUI Wallet on Mac OS

1. Download the Wallet image file 
[HorizenWallet-0.85.1.dmg](https://github.com/HorizenOfficial/zencash-swing-wallet-ui/releases/download/0.85.1/HorizenWallet-0.85.1.dmg). 

2. Security check: You may decide to run a virus scan on it before proceeding... In addition using a tool 
such as [http://quickhash-gui.org/](http://quickhash-gui.org/) you may calculate the its SHA256 checksum. The 
result should be:
```
ae1f0df047695bd7f6150331f70f431bda9a1394d3da394aa6513bc14bc63a7a  HorizenWallet-0.85.1.dmg
```
**If the resulting checksum is not `ae1f0df047695bd7f6150331f70f431bda9a1394d3da394aa6513bc14bc63a7a` then**
**something is wrong and you should discard the downloaded wallet!**

3. Install the wallet like any other downloaded Mac OS application: Open the disk image `HorizenWallet-0.85.1.dmg`
and copy the HorizenWallet application to the Applications folder. You can then discard the disk image.
   
### Running the Horizen Desktop GUI Wallet on Mac OS

Simply click on HorizenWallet in the Mac OS application launchpad.

### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

### Some known issues and limitations
1. Limitation: The list of transactions does not show all outgoing ones (specifically outgoing Z address 
transactions).  
1. Limitation: if two users exchange text messages via the messaging UI TAB and one of them has a system clock, substantially running slow or fast by more than 1 minute, it is possible that this user will see text messages appearing out of order. 
1. Limitation: if a messaging identity has been created (happens on first click on the messaging UI tab), then replacing the `wallet.dat` or changing the node configuration between mainnet and testnet will make the identity invalid. This will result in a wallet update error. To remove the error the directory `~/.HorizenSwingWalletUI/messaging` may be manually renamed or deleted (when the wallet is stopped). **CAUTION: all messaging history will be lost in this case!**
