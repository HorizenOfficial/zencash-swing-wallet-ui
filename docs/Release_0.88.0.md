## [Horizen](https://horizen.global/) Desktop GUI Wallet binary release 0.88.0

It includes [Horizen 2.0.19 binaries](https://github.com/ZencashOfficial/zen/releases/tag/v2.0.19). 

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

It requires a 64-bit Windows 7 or later version to run.

1. Download the Wallet EXE file
[HorizenDesktopGUIWallet-0.88.0.exe](https://github.com/ZencashOfficial/zencash-swing-wallet-ui/releases/download/0.88.0/HorizenDesktopGUIWallet-0.88.0.exe).

2. Security check: You may decide to run a virus scan on it, before proceeding... In addition using a tool 
such as [http://quickhash-gui.org/](http://quickhash-gui.org/) you may calculate the its SHA256 checksum. The 
result should be:
```
58ce0261f80d49dacd3e5542c1282e44fba377c4f8ab7fa51d29c61e06e04df9  HorizenDesktopGUIWallet-0.88.0.exe
```
**If the resulting checksum is not `58ce0261f80d49dacd3e5542c1282e44fba377c4f8ab7fa51d29c61e06e04df9` then**
**something is wrong and you should discard the downloaded wallet!**

3. Run the `HorizenDesktopGUIWallet-0.88.0.exe` installer and choose an installation folder.
   
### Running the Horizen Desktop GUI Wallet on Windows

Double click on `HorizenDesktopGUIWallet.exe` in the installation folder or run `HorizenDesktopGUIWallet` from the start menu.
On first run (only) the wallet will download the cryptographic keys (1.6GB or so).
In case of problems logs are written in `%LOCALAPPDATA%\ZENCashSwingWalletUI\` for diagnostics.

### Installing the Horizen Desktop GUI Wallet on Mac OS

It requires Mac OS Sierra/High Sierra/Mojave.

1. Download the Wallet image file
[HorizenDesktopGUIWallet-0.88.0.dmg](https://github.com/ZencashOfficial/zencash-swing-wallet-ui/releases/download/0.88.0/HorizenDesktopGUIWallet-0.88.0.dmg).

2. Security check: You may decide to run a virus scan on it before proceeding... In addition using a tool
such as [http://quickhash-gui.org/](http://quickhash-gui.org/) you may calculate the its SHA256 checksum. The
result should be:
```
7f7956ed954591826b50999898b9b6ada8dc0839595e09f4eedc2feff4526e50  HorizenDesktopGUIWallet-0.88.0.dmg
```
**If the resulting checksum is not `7f7956ed954591826b50999898b9b6ada8dc0839595e09f4eedc2feff4526e50` then**
**something is wrong and you should discard the downloaded wallet!**

3. Install the wallet like any other downloaded Mac OS application: Open the disk image `HorizenWallet-0.88.0.dmg`
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
1. If a system has a high resolution monitor with DPI scaling enabled, not all GUI elements scale alike.
As a result the Wallet UI may feel inconvenient to use at scaling above 1.5x or even unusable at scaling above 3x.
This problem will be fixed in future versions.
1. Limitation: The list of transactions does not show all outgoing ones (specifically outgoing Z address 
transactions).  
