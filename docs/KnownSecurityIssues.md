## [ZENCash](https://zensystem.io/) Desktop GUI Wallet known security issues

This document describes various security-related vulnerabilities in the GUI wallet that have become known after release and users should be aware of them!

### Watch-only addresses

This issue affects all wallet versions below 0.75.9.
If the wallet (`wallet.dat` file) contains a watch-only address, this address is shown in the "Own Addresses" UI TAB of the GUI wallet,
thus creating the impression to the user that he is in control of the address. In fact watch only addresses have no private key in the
wallet and their balance cannot be spent. To avoid confusion with watch-only addresses, users should not import watch-only addresses into
the wallet. In addition users should make sure they are in control of all addresses that are used as receiving addresses for ZEN. If in 
doubt whether a certain address is under user control, the user may obtain its private key via the wallet UI. If the private key is known/
obtainable then the address belongs to the wallet and its balance is spend-able.

### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
