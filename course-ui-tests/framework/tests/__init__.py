"""framework 用例包。

这个 __init__.py 不只是标记包：lesson11 根目录有同名的
test_ui_action_api_verify.py，没有包结构时 pytest 会因
模块重名拒绝收集；有了它，本目录模块以 tests.xxx 全限定名导入，不再冲突。
"""
