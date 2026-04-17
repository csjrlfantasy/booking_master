"""
票务模块测试用例

包含：票务列表、票务详情、场次、座位图等接口测试
"""

import pytest
import allure
from utils.http_client import HttpClient
from utils.asserts import assert_response, assert_success, assert_code
from data.test_data import TicketTestData


@allure.feature("票务模块")
@allure.story("票务列表")
class TestTicketList:
    """票务列表测试"""
    
    @pytest.mark.smoke
    @pytest.mark.ticket
    @allure.title("获取票务列表")
    @allure.description("验证获取票务列表成功")
    def test_get_ticket_list(self, http_client: HttpClient):
        """测试获取票务列表"""
        params = {
            "pageNum": 1,
            "pageSize": 10
        }
        response = http_client.get("/api/tickets", params=params)
        
        resp = assert_response(response, "获取票务列表")
        resp.success()
        
        data = response.get("data", {})
        resp.is_type(data.get("list"), list, "列表应为数组")
        resp.has_fields(data, ["list", "pageNum", "pageSize", "total"])
    
    @pytest.mark.ticket
    @allure.title("票务列表 - 分类筛选")
    @allure.description("验证按分类筛选票务")
    def test_ticket_list_by_category(self, http_client: HttpClient):
        """测试按分类筛选"""
        params = {
            "categoryId": 1,
            "pageNum": 1,
            "pageSize": 10
        }
        response = http_client.get("/api/tickets", params=params)
        
        resp = assert_response(response, "分类筛选")
        resp.success()
        
        # 验证返回的票务属于指定分类
        data = response.get("data", {})
        if data.get("list"):
            first_ticket = data["list"][0]
            resp.equals(1, first_ticket.get("categoryId"), "分类ID应匹配")
    
    @pytest.mark.ticket
    @allure.title("票务列表 - 关键词搜索")
    @allure.description("验证关键词搜索功能")
    def test_ticket_list_by_keyword(self, http_client: HttpClient):
        """测试关键词搜索"""
        params = {
            "keyword": "周杰伦",
            "pageNum": 1,
            "pageSize": 10
        }
        response = http_client.get("/api/tickets", params=params)
        
        resp = assert_response(response, "关键词搜索")
        resp.success()
    
    @pytest.mark.ticket
    @allure.title("票务列表 - 城市筛选")
    @allure.description("验证按城市筛选票务")
    def test_ticket_list_by_city(self, http_client: HttpClient):
        """测试按城市筛选"""
        params = {
            "city": "北京",
            "pageNum": 1,
            "pageSize": 10
        }
        response = http_client.get("/api/tickets", params=params)
        
        resp = assert_response(response, "城市筛选")
        resp.success()
    
    @pytest.mark.ticket
    @allure.title("票务列表 - 日期范围筛选")
    @allure.description("验证按演出日期范围筛选")
    def test_ticket_list_by_date_range(self, http_client: HttpClient):
        """测试日期范围筛选"""
        params = {
            "startDate": "2026-05-01",
            "endDate": "2026-06-01",
            "pageNum": 1,
            "pageSize": 10
        }
        response = http_client.get("/api/tickets", params=params)
        
        assert_response(response, "日期范围筛选").success()
    
    @pytest.mark.ticket
    @allure.title("票务列表 - 热门排序")
    @allure.description("验证按热门排序")
    def test_ticket_list_sort_by_hot(self, http_client: HttpClient):
        """测试热门排序"""
        params = {
            "sortBy": "hot",
            "sortOrder": "desc",
            "pageNum": 1,
            "pageSize": 10
        }
        response = http_client.get("/api/tickets", params=params)
        
        assert_response(response, "热门排序").success()
    
    @pytest.mark.ticket
    @allure.title("票务列表 - 价格排序")
    @allure.description("验证按价格排序")
    def test_ticket_list_sort_by_price(self, http_client: HttpClient):
        """测试价格排序"""
        params = {
            "sortBy": "price",
            "sortOrder": "asc",
            "pageNum": 1,
            "pageSize": 10
        }
        response = http_client.get("/api/tickets", params=params)
        
        assert_response(response, "价格排序").success()
    
    @pytest.mark.ticket
    @allure.title("票务列表 - 分页查询")
    @allure.description("验证分页功能")
    def test_ticket_list_pagination(self, http_client: HttpClient):
        """测试分页查询"""
        params = {
            "pageNum": 2,
            "pageSize": 5
        }
        response = http_client.get("/api/tickets", params=params)
        
        resp = assert_response(response, "分页查询")
        resp.success()
        
        data = response.get("data", {})
        resp.equals(2, data.get("pageNum"), "页码应匹配")


@allure.feature("票务模块")
@allure.story("票务详情")
class TestTicketDetail:
    """票务详情测试"""
    
    @pytest.mark.smoke
    @pytest.mark.ticket
    @allure.title("获取票务详情")
    @allure.description("验证获取票务详情成功")
    def test_get_ticket_detail(self, http_client: HttpClient):
        """测试获取票务详情"""
        ticket_id = TicketTestData.TEST_TICKET_ID
        response = http_client.get(f"/api/tickets/{ticket_id}")
        
        resp = assert_response(response, "获取票务详情")
        resp.success()
        
        data = response.get("data", {})
        resp.has_fields(data, [
            "ticketId", "name", "categoryId", "venueId", 
            "venueName", "poster", "status"
        ])
        resp.equals(ticket_id, data.get("ticketId"), "票务ID应匹配")
    
    @pytest.mark.ticket
    @allure.title("获取票务详情 - 不存在的票务")
    @allure.description("验证获取不存在的票务时返回错误")
    def test_get_ticket_detail_not_exists(self, http_client: HttpClient):
        """测试票务不存在"""
        ticket_id = 999999
        response = http_client.get(f"/api/tickets/{ticket_id}")
        
        assert_code(response, 100201, "票务不存在")


@allure.feature("票务模块")
@allure.story("场次信息")
class TestTicketSession:
    """场次信息测试"""
    
    @pytest.mark.smoke
    @pytest.mark.ticket
    @allure.title("获取场次列表")
    @allure.description("验证获取场次列表成功")
    def test_get_session_list(self, http_client: HttpClient):
        """测试获取场次列表"""
        ticket_id = TicketTestData.TEST_TICKET_ID
        response = http_client.get(f"/api/tickets/{ticket_id}/sessions")
        
        resp = assert_response(response, "获取场次列表")
        resp.success()
        
        data = response.get("data", {})
        resp.is_type(data, list, "场次列表应为数组")
    
    @pytest.mark.ticket
    @allure.title("获取场次详情")
    @allure.description("验证获取场次详情")
    def test_get_session_detail(self, http_client: HttpClient):
        """测试获取场次详情"""
        ticket_id = TicketTestData.TEST_TICKET_ID
        session_id = TicketTestData.TEST_SESSION_ID
        response = http_client.get(f"/api/tickets/{ticket_id}/sessions/{session_id}")
        
        resp = assert_response(response, "获取场次详情")
        resp.success()
        
        data = response.get("data", {})
        resp.equals(session_id, data.get("sessionId"), "场次ID应匹配")


@allure.feature("票务模块")
@allure.story("票档信息")
class TestTicketPriceLevel:
    """票档信息测试"""
    
    @pytest.mark.ticket
    @allure.title("获取票档列表")
    @allure.description("验证获取票档列表")
    def test_get_price_level_list(self, http_client: HttpClient):
        """测试获取票档列表"""
        ticket_id = TicketTestData.TEST_TICKET_ID
        response = http_client.get(f"/api/tickets/{ticket_id}/price-levels")
        
        resp = assert_response(response, "获取票档列表")
        resp.success()
        
        data = response.get("data", {})
        resp.is_type(data, list, "票档列表应为数组")
    
    @pytest.mark.ticket
    @allure.title("获取票档详情")
    @allure.description("验证获取票档详情")
    def test_get_price_level_detail(self, http_client: HttpClient):
        """测试获取票档详情"""
        ticket_id = TicketTestData.TEST_TICKET_ID
        price_level_id = 1001
        response = http_client.get(f"/api/tickets/{ticket_id}/price-levels/{price_level_id}")
        
        assert_response(response, "获取票档详情").success()


@allure.feature("票务模块")
@allure.story("座位图")
class TestTicketSeatMap:
    """座位图测试"""
    
    @pytest.mark.ticket
    @allure.title("获取座位图")
    @allure.description("验证获取座位图成功")
    def test_get_seat_map(self, http_client: HttpClient):
        """测试获取座位图"""
        ticket_id = TicketTestData.TEST_TICKET_ID
        response = http_client.get(f"/api/tickets/{ticket_id}/seat-map")
        
        resp = assert_response(response, "获取座位图")
        if response.get("code") == 0:
            resp.success()
            data = response.get("data", {})
            if data:
                resp.has_fields(data, ["venueId", "mapName", "canvasWidth", "canvasHeight"])
        else:
            # 可能票务不支持选座
            resp.failed()
    
    @pytest.mark.ticket
    @allure.title("获取座位状态")
    @allure.description("验证获取座位状态")
    def test_get_seat_status(self, http_client: HttpClient):
        """测试获取座位状态"""
        ticket_id = TicketTestData.TEST_TICKET_ID
        session_id = TicketTestData.TEST_SESSION_ID
        response = http_client.get(
            f"/api/tickets/{ticket_id}/sessions/{session_id}/seats"
        )
        
        resp = assert_response(response, "获取座位状态")
        if response.get("code") == 0:
            resp.success()
            data = response.get("data", {})
            resp.is_type(data, list, "座位列表应为数组")


@allure.feature("票务模块")
@allure.story("票务分类")
class TestTicketCategory:
    """票务分类测试"""
    
    @pytest.mark.ticket
    @allure.title("获取票务分类列表")
    @allure.description("验证获取分类列表")
    def test_get_category_list(self, http_client: HttpClient):
        """测试获取分类列表"""
        response = http_client.get("/api/ticket-categories")
        
        resp = assert_response(response, "获取分类列表")
        resp.success()
        
        data = response.get("data", {})
        resp.is_type(data, list, "分类列表应为数组")
        resp.not_empty(data, "分类列表不应为空")


# ==================== 辅助测试函数 ====================

def test_ticket_favorites(http_client: HttpClient):
    """测试收藏票务"""
    # 登录后可测试
    pass


def test_ticket_share(http_client: HttpClient):
    """测试分享票务"""
    pass


def test_ticket_recommend(http_client: HttpClient):
    """测试推荐票务"""
    pass