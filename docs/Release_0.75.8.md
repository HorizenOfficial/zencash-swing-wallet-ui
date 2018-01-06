## [ZENCash](https://zensystem.io/) Desktop GUI Wallet binary release 0.75.8 for Windows

This is a [ZENCash](https://zensystem.io/) Desktop GUI Wallet binary release 0.75.8 for Windows. 
It requires a 64-bit Windows 7 or later version to run. It includes [ZENCash 2.0.10-1 binaries](https://github.com/ZencashOfficial/zen/releases/tag/v2.0.10-1)

### IMPORTANT: Please read the [security notice](KnownSecurityIssues.md) about watch-only addresses before using the wallet!

### Installing the ZENCash Desktop GUI Wallet on Windows

1. Download the Wallet ZIP file 
[ZENCashDesktopGUIWallet_0.75.8.zip](https://github.com/ZencashOfficial/zencash-swing-wallet-ui/releases/download/0.75.8/ZENCashDesktopGUIWallet_0.75.8.zip). 

2. Security check: You may decide to run a virus scan on it, before proceeding... In addition using a tool 
such as [http://quickhash-gui.org/](http://quickhash-gui.org/) you may calculate the its SHA256 checksum. The 
result should be:
```
7d4ef3973aba20ad0d7961e0eef61c2e80b85c4049cec75b8171b5e26338b27b ZENCashDesktopGUIWallet_0.75.8.zip
```
**If the resulting checksum is not `7d4ef3973aba20ad0d7961e0eef61c2e80b85c4049cec75b8171b5e26338b27b` then**
**something is wrong and you should discard the downloaded wallet!**

3. Unzip the Wallet ZIP file `ZENCashDesktopGUIWallet_0.75.8.zip` in some directory that it will run from.
   
### Running the ZENCash Desktop GUI Wallet on Windows

Double click on `ZENCashDesktopGUIWallet.exe`. On first run (only) the wallet will download the cryptographic keys 
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
1. Limitation: The list of transactions does not show all outgoing ones (specifically outgoing Z address 
transactions).  
