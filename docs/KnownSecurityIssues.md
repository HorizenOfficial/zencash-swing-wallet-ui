## [ZENCash](https://zensystem.io/) Desktop GUI Wallet known security issues

This document describes various security-related vulnerabilities in the GUI wallet that have become known after release and users should be aware of them!

### Watch-only addresses

This issue affects all wallet versions below 0.76.
If the wallet (`wallet.dat` file) contains a watch-only address, this address is shown in the "Own Addresses" UI TAB of the GUI wallet,
thus creating the impression to the user that he is in control of the address. In fact watch only addresses have no private key in the
wallet and their balance cannot be spent. To avoid confusion with watch-only addresses, users should not import watch-only addresses into
the wallet. In addition users should make sure they are in control of all addresses that are used as receiving addresses for ZEN. If in 
doubt whether a certain address is under user control, the user may obtain its private key via the wallet UI. If the private key is known/
obtainable then the address belongs to the wallet and its balance is spend-able.

### Compatibility with other applications

The ZENCash Desktop GUI Wallet is not compatible with applications that modify the ZEN `wallet.dat` file. The wallet should not be used
with such applications on the same PC. For instance some distributed exchange applications are known to create watch-only addresses in the
`wallet.dat` file that cause the GUI wallet to display a wrong balance and/or display addresses that do not belong to the wallet. 

### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
