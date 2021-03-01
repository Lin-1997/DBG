# DBG
DBG算法字符串拼接

给定一个字符串 S (|S|>10^9)，字符串由A~Z 26个大写字母组成。对S执行6*10^10次随机抽取子串，每次抽取的子串长度为60-100个字符(随机)，所有子串形成的子串集合记为£。
该算法用于将£拼接成字符串S'，目标是S’=S。

当S无超过最小抽取长度60的重复子串时，该算法能得出S'=S。

参考文献：

Pevzner, Pavel A. and Tang, Haixu and Waterman, Michael S. "An Eulerian path approach to DNA fragment assembly". https://www.pnas.org/content/98/17/9748
