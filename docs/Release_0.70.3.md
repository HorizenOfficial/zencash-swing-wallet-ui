## [ZENCash](https://zensystem.io/) Desktop GUI Wallet binary release 0.70.3 for Windows

This is a [ZENCash](https://zensystem.io/) Desktop GUI Wallet binary release 0.70.3 for Windows. 
It requires a 64-bit Windows 7 or later version to run. It also includes 
[ZENCash 2.0.9-3 binaries](https://github.com/ZencashOfficial/zen/releases/tag/v2.0.9-3-b8d2ebf) by 
[@cronicc](https://github.com/cronicc). 

![Screenshot](ZENCashWalletWindows.png "Main Window")

### Installing the ZENCash Desktop GUI Wallet on Windows

1. Download the Wallet ZIP file 
[ZENCashWallet_0.70.3_Windows.zip]https://github.com/vaklinov/zencash-swing-wallet-ui/releases/download/0.70.3/ZENCashWallet_0.70.3_Windows.zip). 

2. Security check: You may decide to run a virus scan on it, before proceeding... In addition using a tool 
such as [http://quickhash-gui.org/](http://quickhash-gui.org/) you may calculate the its SHA256 checksum. The 
result should be:
```
47cbf70aa82c297f2f2e2c651df3d78f2458a44302b3bf2ad1d66b024222db5b *ZENCashWallet_0.70.3_Windows.zip
```
**If the resulting checksum is not `47cbf70aa82c297f2f2e2c651df3d78f2458a44302b3bf2ad1d66b024222db5b` then**
**something is wrong and you should discard the downloaded wallet!**

3. Unzip the Wallet ZIP file ZENCashWallet_0.70.3_Windows.zip in some directory that it will run from.
   
### Running the ZENCash Desktop GUI Wallet on Windows

Double click on `ZENCashSwingWalletUI.exe`. On first run (only) the wallet will download the cryptographic keys 
(900MB or so). In case of problems logs are written in `%LOCALAPPDATA%\ZENCashSwingWalletUI\` for diagnostics.


### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

### Known issues and limitations
1. If a system has a high resolution monitor with DPI scaling enabled, not all GUI elements scale alike.
As a result the Wallet UI may feel inconvenient to use at scaling above 1.5x or even unusable at scaling above 3x.
This problem will be fixed in future versions.
1. In rare cases users have reported problems
when running the GUI wallet using certain ATI video drivers/cards. If such a problem is encountered then a 
user may run `ZENCashSwingWalletUI.jar` instead of `ZENCashSwingWalletUI.exe`. This JAR file will be runnable 
only if there is a Java JDK installed separately on the system. To install JDK 8 for Windows you may use 
this [good tutorial](http://www.wikihow.com/Install-the-Java-Software-Development-Kit)
1. Issue: GUI data tables (transactions/addresses etc.) allow copying of data via double click but also allow editing. 
The latter needs to be disabled. 
1. Limitation: The list of transactions does not show all outgoing ones (specifically outgoing Z address 
transactions).  
