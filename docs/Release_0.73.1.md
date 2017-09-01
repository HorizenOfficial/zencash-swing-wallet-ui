## [ZENCash](https://zensystem.io/) Desktop GUI Wallet binary release 0.73.1 for Windows

This is a [ZENCash](https://zensystem.io/) Desktop GUI Wallet binary release 0.73.1 for Windows. 
It requires a 64-bit Windows 7 or later version to run. It also includes [ZENCash 2.0.9-4 binaries](https://github.com/ZencashOfficial/zen/releases/tag/v2.0.9-4) by [@cronicc](https://github.com/cronicc). 

This release includes an initial/early version of the one-to-one ZEN messaging functionality. The messaging 
UI TAB allows users to securely exchange text messages. This release does not yet include features such as 
group messaging or IPFS integration (will be developed in future releases). 

![Screenshot](ZENChat.png "Main Window")

### Installing the ZENCash Desktop GUI Wallet on Windows

1. Download the Wallet ZIP file 
[ZENCashSwingWalletUI_0.73.1.zip](https://github.com/ZencashOfficial/zencash-swing-wallet-ui/releases/download/0.73.1/ZENCashSwingWalletUI_0.73.1.zip). 

2. Security check: You may decide to run a virus scan on it, before proceeding... In addition using a tool 
such as [http://quickhash-gui.org/](http://quickhash-gui.org/) you may calculate the its SHA256 checksum. The 
result should be:
```
1ec2c45e86295031fdd663850ad55a4e4a7b876ec6a3e62dd0c9db115dae9223  ZENCashSwingWalletUI_0.73.1.zip
```
**If the resulting checksum is not `1ec2c45e86295031fdd663850ad55a4e4a7b876ec6a3e62dd0c9db115dae9223` then**
**something is wrong and you should discard the downloaded wallet!**

3. Unzip the Wallet ZIP file `ZENCashSwingWalletUI_0.73.1.zip` in some directory that it will run from.
   
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
1. Limitation: The list of transactions does not show all outgoing ones (specifically outgoing Z address 
transactions).  
