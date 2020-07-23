## [Horizen](https://horizen.global/) Desktop GUI Wallet binary release 0.85.0 for Windows

This is a [Horizen](https://horizen.global/) Desktop GUI Wallet binary release 0.85.0 for Windows. 
It requires a 64-bit Windows 7 or later version to run. It includes [Horizen 2.0.16 binaries](https://github.com/HorizenOfficial/zen/releases/tag/v2.0.16). 

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

### Installing the Horizen Desktop GUI Wallet on Windows

1. Download the Wallet ZIP file 
[HorizenDesktopGUIWallet_0.85.0.zip](https://github.com/HorizenOfficial/zencash-swing-wallet-ui/releases/download/0.85.0/HorizenDesktopGUIWallet_0.85.0.zip). 

2. Security check: You may decide to run a virus scan on it, before proceeding... In addition using a tool 
such as [http://quickhash-gui.org/](http://quickhash-gui.org/) you may calculate the its SHA256 checksum. The 
result should be:
```
facef7a71047851199689606a563fbe9b12c4dcfbdb369340dc388ae239af3c1  HorizenDesktopGUIWallet_0.85.0.zip
```
**If the resulting checksum is not `facef7a71047851199689606a563fbe9b12c4dcfbdb369340dc388ae239af3c1` then**
**something is wrong and you should discard the downloaded wallet!**

3. Unzip the Wallet ZIP file `HorizenDesktopGUIWallet_0.85.0.zip` in some directory that it will run from.
   
### Running the Horizen Desktop GUI Wallet on Windows

Double click on `HorizenDesktopGUIWallet.exe`. On first run (only) the wallet will download the cryptographic keys 
(1.6GB or so). In case of problems logs are written in `%LOCALAPPDATA%\ZENCashSwingWalletUI\` for diagnostics.


### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

### Some known issues and limitations
1. If a system has a high resolution monitor with DPI scaling enabled, not all GUI elements scale alike.
As a result the Wallet UI may feel inconvenient to use at scaling above 1.5x or even unusable at scaling above 3x.
This problem will be fixed in future versions.
1. Limitation: The list of transactions does not show all outgoing ones (specifically outgoing Z address 
transactions).  
