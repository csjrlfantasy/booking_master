"""
自定义断言模块

提供统一的响应断言、字段验证、错误码验证等功能
"""

from typing import Any, Dict, List, Optional, Union
import jsonschema

from .logger import get_logger

logger = get_logger(__name__)


class AssertError(Exception):
    """断言错误异常"""
    pass


class ResponseAssert:
    """
    响应断言类
    
    提供对API响应的各种断言方法
    """
    
    def __init__(self, response: Dict[str, Any], description: str = ""):
        """
        初始化断言
        
        Args:
            response: API响应字典
            description: 断言描述（用于日志）
        """
        self.response = response
        self.description = description
        
    @property
    def code(self) -> int:
        """获取响应码"""
        return self.response.get("code", -1)
    
    @property
    def message(self) -> str:
        """获取响应消息"""
        return self.response.get("message", "")
    
    @property
    def data(self) -> Any:
        """获取响应数据"""
        return self.response.get("data")
    
    def _log_assert(self, passed: bool, msg: str):
        """记录断言结果"""
        status = "✓" if passed else "✗"
        log_msg = f"[{status}] {self.description}: {msg}"
        if passed:
            logger.debug(log_msg)
        else:
            logger.error(log_msg)
            
    # ==================== 基本断言 ====================
    
    def success(self, msg: str = "Response should be successful") -> "ResponseAssert":
        """断言响应成功(code=0)"""
        passed = self.code == 0
        self._log_assert(passed, f"code={self.code}, {msg}")
        if not passed:
            raise AssertError(f"Expected code=0, got {self.code}: {self.message}")
        return self
    
    def failed(self, msg: str = "Response should fail") -> "ResponseAssert":
        """断言响应失败(code!=0)"""
        passed = self.code != 0
        self._log_assert(passed, f"code={self.code}, {msg}")
        if not passed:
            raise AssertError(f"Expected code!=0, got {self.code}")
        return self
    
    def equals(self, expected: Any, actual: Any, 
               msg: str = "Values should be equal") -> "ResponseAssert":
        """断言值相等"""
        passed = expected == actual
        self._log_assert(passed, f"{msg}: expected={expected}, actual={actual}")
        if not passed:
            raise AssertError(f"{msg}: expected={expected}, actual={actual}")
        return self
    
    def not_equals(self, unexpected: Any, actual: Any,
                   msg: str = "Values should not be equal") -> "ResponseAssert":
        """断言值不相等"""
        passed = unexpected != actual
        self._log_assert(passed, f"{msg}: unexpected={unexpected}, actual={actual}")
        if not passed:
            raise AssertError(f"{msg}: unexpected={unexpected}, actual={actual}")
        return self
    
    def is_none(self, value: Any, msg: str = "Value should be None") -> "ResponseAssert":
        """断言值为None"""
        passed = value is None
        self._log_assert(passed, f"{msg}: value={value}")
        if not passed:
            raise AssertError(f"{msg}: value is {value}")
        return self
    
    def not_none(self, value: Any, msg: str = "Value should not be None") -> "ResponseAssert":
        """断言值不为None"""
        passed = value is not None
        self._log_assert(passed, f"{msg}: value={value}")
        if not passed:
            raise AssertError(f"{msg}: value is None")
        return self
    
    def is_true(self, value: bool, msg: str = "Value should be True") -> "ResponseAssert":
        """断言值为True"""
        passed = value is True
        self._log_assert(passed, f"{msg}: value={value}")
        if not passed:
            raise AssertError(f"{msg}: value is {value}")
        return self
    
    def is_false(self, value: bool, msg: str = "Value should be False") -> "ResponseAssert":
        """断言值为False"""
        passed = value is False
        self._log_assert(passed, f"{msg}: value={value}")
        if not passed:
            raise AssertError(f"{msg}: value is {value}")
        return self
    
    # ==================== 类型断言 ====================
    
    def is_type(self, value: Any, expected_type: type,
                msg: str = "Value should be of expected type") -> "ResponseAssert":
        """断言值类型"""
        passed = isinstance(value, expected_type)
        self._log_assert(passed, f"{msg}: expected={expected_type.__name__}, actual={type(value).__name__}")
        if not passed:
            raise AssertError(f"{msg}: expected={expected_type.__name__}, actual={type(value).__name__}")
        return self
    
    def is_list(self, value: Any, msg: str = "Value should be a list") -> "ResponseAssert":
        """断言值是列表"""
        return self.is_type(value, list, msg)
    
    def is_dict(self, value: Any, msg: str = "Value should be a dict") -> "ResponseAssert":
        """断言值是字典"""
        return self.is_type(value, dict, msg)
    
    def is_string(self, value: Any, msg: str = "Value should be a string") -> "ResponseAssert":
        """断言值是字符串"""
        return self.is_type(value, str, msg)
    
    def is_int(self, value: Any, msg: str = "Value should be an int") -> "ResponseAssert":
        """断言值是整数"""
        return self.is_type(value, int, msg)
    
    def is_number(self, value: Any, msg: str = "Value should be a number") -> "ResponseAssert":
        """断言值是数字(int或float)"""
        passed = isinstance(value, (int, float)) and not isinstance(value, bool)
        self._log_assert(passed, f"{msg}: actual={type(value).__name__}")
        if not passed:
            raise AssertError(f"{msg}: actual={type(value).__name__}")
        return self
    
    # ==================== 字符串断言 ====================
    
    def contains(self, container: Union[str, list, dict], substring: Any,
                 msg: str = "Container should contain substring") -> "ResponseAssert":
        """断言包含关系"""
        passed = substring in container
        self._log_assert(passed, f"{msg}: container={container}, substring={substring}")
        if not passed:
            raise AssertError(f"{msg}: container={container}, substring={substring}")
        return self
    
    def not_contains(self, container: Union[str, list, dict], substring: Any,
                      msg: str = "Container should not contain substring") -> "ResponseAssert":
        """断言不包含关系"""
        passed = substring not in container
        self._log_assert(passed, f"{msg}: container={container}, substring={substring}")
        if not passed:
            raise AssertError(f"{msg}: container={container}, substring={substring}")
        return self
    
    def starts_with(self, text: str, prefix: str,
                    msg: str = "Text should start with prefix") -> "ResponseAssert":
        """断言字符串开头"""
        passed = text.startswith(prefix)
        self._log_assert(passed, f"{msg}: text={text}, prefix={prefix}")
        if not passed:
            raise AssertError(f"{msg}: text={text}, prefix={prefix}")
        return self
    
    def ends_with(self, text: str, suffix: str,
                  msg: str = "Text should end with suffix") -> "ResponseAssert":
        """断言字符串结尾"""
        passed = text.endswith(suffix)
        self._log_assert(passed, f"{msg}: text={text}, suffix={suffix}")
        if not passed:
            raise AssertError(f"{msg}: text={text}, suffix={suffix}")
        return self
    
    def regex_match(self, text: str, pattern: str,
                    msg: str = "Text should match regex") -> "ResponseAssert":
        """断言正则匹配"""
        import re
        passed = re.search(pattern, text) is not None
        self._log_assert(passed, f"{msg}: text={text}, pattern={pattern}")
        if not passed:
            raise AssertError(f"{msg}: text={text}, pattern={pattern}")
        return self
    
    # ==================== 数值断言 ====================
    
    def greater_than(self, value: Union[int, float], threshold: Union[int, float],
                     msg: str = "Value should be greater than threshold") -> "ResponseAssert":
        """断言大于"""
        passed = value > threshold
        self._log_assert(passed, f"{msg}: value={value}, threshold={threshold}")
        if not passed:
            raise AssertError(f"{msg}: value={value}, threshold={threshold}")
        return self
    
    def less_than(self, value: Union[int, float], threshold: Union[int, float],
                  msg: str = "Value should be less than threshold") -> "ResponseAssert":
        """断言小于"""
        passed = value < threshold
        self._log_assert(passed, f"{msg}: value={value}, threshold={threshold}")
        if not passed:
            raise AssertError(f"{msg}: value={value}, threshold={threshold}")
        return self
    
    def greater_or_equal(self, value: Union[int, float], threshold: Union[int, float],
                         msg: str = "Value should be >= threshold") -> "ResponseAssert":
        """断言大于等于"""
        passed = value >= threshold
        self._log_assert(passed, f"{msg}: value={value}, threshold={threshold}")
        if not passed:
            raise AssertError(f"{msg}: value={value}, threshold={threshold}")
        return self
    
    def less_or_equal(self, value: Union[int, float], threshold: Union[int, float],
                      msg: str = "Value should be <= threshold") -> "ResponseAssert":
        """断言小于等于"""
        passed = value <= threshold
        self._log_assert(passed, f"{msg}: value={value}, threshold={threshold}")
        if not passed:
            raise AssertError(f"{msg}: value={value}, threshold={threshold}")
        return self
    
    # ==================== 集合断言 ====================
    
    def length_equal(self, collection: Union[str, list, dict], length: int,
                     msg: str = "Collection length should equal") -> "ResponseAssert":
        """断言集合长度"""
        passed = len(collection) == length
        self._log_assert(passed, f"{msg}: length={len(collection)}, expected={length}")
        if not passed:
            raise AssertError(f"{msg}: length={len(collection)}, expected={length}")
        return self
    
    def length_greater(self, collection: Union[str, list, dict], length: int,
                      msg: str = "Collection length should be greater") -> "ResponseAssert":
        """断言集合长度大于"""
        passed = len(collection) > length
        self._log_assert(passed, f"{msg}: length={len(collection)}, min={length}")
        if not passed:
            raise AssertError(f"{msg}: length={len(collection)}, min={length}")
        return self
    
    def length_less(self, collection: Union[str, list, dict], length: int,
                   msg: str = "Collection length should be less") -> "ResponseAssert":
        """断言集合长度小于"""
        passed = len(collection) < length
        self._log_assert(passed, f"{msg}: length={len(collection)}, max={length}")
        if not passed:
            raise AssertError(f"{msg}: length={len(collection)}, max={length}")
        return self
    
    def is_empty(self, collection: Union[str, list, dict],
                 msg: str = "Collection should be empty") -> "ResponseAssert":
        """断言集合为空"""
        passed = len(collection) == 0
        self._log_assert(passed, f"{msg}: length={len(collection)}")
        if not passed:
            raise AssertError(f"{msg}: length={len(collection)}")
        return self
    
    def not_empty(self, collection: Union[str, list, dict],
                  msg: str = "Collection should not be empty") -> "ResponseAssert":
        """断言集合不为空"""
        passed = len(collection) > 0
        self._log_assert(passed, f"{msg}: length={len(collection)}")
        if not passed:
            raise AssertError(f"{msg}: collection is empty")
        return self
    
    def in_collection(self, item: Any, collection: List,
                      msg: str = "Item should be in collection") -> "ResponseAssert":
        """断言元素在列表中"""
        passed = item in collection
        self._log_assert(passed, f"{msg}: item={item}, collection={collection}")
        if not passed:
            raise AssertError(f"{msg}: item={item}, collection={collection}")
        return self
    
    # ==================== 响应特定断言 ====================
    
    def code_equals(self, expected_code: int,
                    msg: str = "Response code should equal") -> "ResponseAssert":
        """断言响应码等于指定值"""
        return self.equals(expected_code, self.code, msg)
    
    def message_contains(self, substring: str,
                         msg: str = "Response message should contain") -> "ResponseAssert":
        """断言响应消息包含指定文本"""
        return self.contains(self.message, substring, msg)
    
    def data_not_none(self, msg: str = "Response data should not be None") -> "ResponseAssert":
        """断言响应数据不为空"""
        return self.not_none(self.data, msg)
    
    def data_is_none(self, msg: str = "Response data should be None") -> "ResponseAssert":
        """断言响应数据为空"""
        return self.is_none(self.data, msg)
    
    def data_is_list(self, msg: str = "Response data should be a list") -> "ResponseAssert":
        """断言响应数据是列表"""
        return self.is_list(self.data, msg)
    
    def data_is_dict(self, msg: str = "Response data should be a dict") -> "ResponseAssert":
        """断言响应数据是字典"""
        return self.is_dict(self.data, msg)
    
    # ==================== 字段存在性断言 ====================
    
    def has_field(self, data: Dict, field: str,
                  msg: str = "Data should have field") -> "ResponseAssert":
        """断言字段存在"""
        passed = field in data
        self._log_assert(passed, f"{msg}: field={field}")
        if not passed:
            raise AssertError(f"{msg}: field={field} not found in {data}")
        return self
    
    def has_fields(self, data: Dict, fields: List[str],
                   msg: str = "Data should have all fields") -> "ResponseAssert":
        """断言多个字段都存在"""
        missing = [f for f in fields if f not in data]
        passed = len(missing) == 0
        self._log_assert(passed, f"{msg}: missing fields={missing}")
        if not passed:
            raise AssertError(f"{msg}: missing fields={missing}")
        return self
    
    # ==================== JSON Schema验证 ====================
    
    def validate_schema(self, data: Dict, schema: Dict,
                        msg: str = "Data should match schema") -> "ResponseAssert":
        """验证JSON Schema"""
        try:
            jsonschema.validate(instance=data, schema=schema)
            self._log_assert(True, f"{msg}: schema validation passed")
        except jsonschema.ValidationError as e:
            self._log_assert(False, f"{msg}: {e.message}")
            raise AssertError(f"{msg}: {e.message}")
        return self
    
    # ==================== 列表项断言 ====================
    
    def list_item_equals(self, data: List, index: int, field: str, expected: Any,
                         msg: str = "List item field should equal") -> "ResponseAssert":
        """断言列表项的字段值"""
        if index >= len(data):
            raise AssertError(f"Index out of range: {index} >= {len(data)}")
        actual = data[index].get(field)
        return self.equals(expected, actual, msg)
    
    def list_item_contains(self, data: List, index: int, field: str, substring: Any,
                          msg: str = "List item field should contain") -> "ResponseAssert":
        """断言列表项的字段包含指定值"""
        if index >= len(data):
            raise AssertError(f"Index out of range: {index} >= {len(data)}")
        actual = data[index].get(field)
        return self.contains(actual, substring, msg)
    
    # ==================== 通用断言 ====================
    
    def that(self, condition: bool, msg: str = "Condition should be True") -> "ResponseAssert":
        """通用条件断言"""
        self._log_assert(condition, msg)
        if not condition:
            raise AssertError(msg)
        return self


# ==================== 便捷函数 ====================

def assert_response(response: Dict, description: str = "") -> ResponseAssert:
    """创建响应断言对象"""
    return ResponseAssert(response, description)


def assert_success(response: Dict, msg: str = "API should return success"):
    """断言响应成功"""
    assert_response(response).success(msg)


def assert_failed(response: Dict, msg: str = "API should return failure"):
    """断言响应失败"""
    assert_response(response).failed(msg)


def assert_code(response: Dict, expected_code: int, msg: str = "Code should equal"):
    """断言响应码"""
    assert_response(response).code_equals(expected_code, msg)


def assert_data_not_none(response: Dict, msg: str = "Data should not be None"):
    """断言数据不为空"""
    assert_response(response).data_not_none(msg)


def assert_list_not_empty(response: Dict, msg: str = "List should not be empty"):
    """断言列表不为空"""
    assert_response(response).data_is_list(msg).not_none(response.get("data"), msg).not_empty(response.get("data"), msg)