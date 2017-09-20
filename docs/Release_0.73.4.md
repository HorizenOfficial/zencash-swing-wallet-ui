## [ZENCash](https://zensystem.io/) Desktop GUI Wallet binary release 0.73.4 for Mac OS

This is a [ZENCash](https://zensystem.io/) Desktop GUI Wallet binary release 0.73.4 for Mac OS. 
It includes [ZENCash 2.0.10 binaries](https://github.com/ZencashOfficial/zen/releases/tag/v2.0.10)
One notable new feature is the ability to send encrypted text messages between users on the ZEN blockchain.

![Screenshot](ZENCashWalletMac_0.73.4.png "Main Window")

### Installing the ZENCash Desktop GUI Wallet on Mac OS

1. Download the Wallet image file 
[ZENCashWallet-0.73.4.dmg](https://github.com/ZencashOfficial/zencash-swing-wallet-ui/releases/download/0.73.4/ZENCashWallet-0.73.4.dmg). 

2. Security check: You may decide to run a virus scan on it before proceeding... In addition using a tool 
such as [http://quickhash-gui.org/](http://quickhash-gui.org/) you may calculate the its SHA256 checksum. The 
result should be:
```
148152571ca14fa054742918dfae0be76875cf74658221b00fe23cf712db1ae4  ZENCashWallet-0.73.4.dmg
```
**If the resulting checksum is not `148152571ca14fa054742918dfae0be76875cf74658221b00fe23cf712db1ae4` then**
**something is wrong and you should discard the downloaded wallet!**

3. You need to (at least temporarily) allow the installation of "apps downloaded from anywhere" on your Mac. 
[This article](http://osxdaily.com/2016/09/27/allow-apps-from-anywhere-macos-gatekeeper/) is a good description
of how to do this. This step will not be necessary in future releases. 

4. Install the wallet like any other downloaded Mac OS application: Open the disk image `ZENCashWallet-0.73.4.dmg`
and copy the ZENCashWallet application to the Applications folder. You can then discard the disk image.
   
### Running the ZENCash Desktop GUI Wallet on Mac OS

Simply click on ZENCashWallet in the Mac OS application launchpad.

### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

### Known issues and limitations
1. Limitation: The list of transactions does not show all outgoing ones (specifically outgoing Z address 
transactions).  
2. Anonymous text messaging does not work reliably in both directions (works reliably in one direction only).
