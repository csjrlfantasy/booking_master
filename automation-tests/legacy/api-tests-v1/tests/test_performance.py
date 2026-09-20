"""
性能测试 - API响应时间测试和并发测试

包含：API响应时间测试、并发处理能力测试、数据库查询性能测试
"""

import pytest
import allure
import time
import statistics
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import List, Dict, Tuple
from utils.http_client import HttpClient
from utils.logger import get_logger

logger = get_logger(__name__)


@allure.feature("性能测试")
@allure.story("API响应时间测试")
class TestAPIResponseTime:
    """API响应时间测试"""
    
    def _measure_response_time(self, client: HttpClient, method: str, path: str, **kwargs) -> Tuple[float, Dict]:
        """
        测量API响应时间
        
        Returns:
            (响应时间(秒), 响应数据)
        """
        start_time = time.time()
        
        if method.upper() == "GET":
            response = client.get(path, **kwargs)
        elif method.upper() == "POST":
            response = client.post(path, **kwargs)
        elif method.upper() == "PUT":
            response = client.put(path, **kwargs)
        elif method.upper() == "DELETE":
            response = client.delete(path, **kwargs)
        else:
            response = {"code": -1, "message": "Unsupported method"}
        
        elapsed_time = time.time() - start_time
        return elapsed_time, response
    
    def _get_performance_stats(self, times: List[float]) -> Dict:
        """计算性能统计"""
        if not times:
            return {}
        
        return {
            "min": min(times),
            "max": max(times),
            "avg": statistics.mean(times),
            "median": statistics.median(times),
            "p95": sorted(times)[int(len(times) * 0.95)] if len(times) >= 20 else max(times),
            "p99": sorted(times)[int(len(times) * 0.99)] if len(times) >= 100 else max(times),
            "std": statistics.stdev(times) if len(times) > 1 else 0
        }
    
    @pytest.mark.performance
    @allure.title("登录接口响应时间测试")
    @allure.description("测试登录接口的响应时间性能")
    def test_login_response_time(self):
        """测试登录接口响应时间"""
        client = HttpClient(base_url="http://localhost:8088")
        response_times = []
        
        # 执行10次登录测试
        for i in range(10):
            elapsed, response = self._measure_response_time(
                client, "POST", "/api/user/login",
                json_data={"username": "test_user", "password": "Test@123456"}
            )
            response_times.append(elapsed)
            logger.info(f"登录第{i+1}次响应时间: {elapsed:.3f}s")
        
        stats = self._get_performance_stats(response_times)
        
        logger.info(f"\n登录接口性能统计:")
        logger.info(f"  最小响应时间: {stats['min']:.3f}s")
        logger.info(f"  最大响应时间: {stats['max']:.3f}s")
        logger.info(f"  平均响应时间: {stats['avg']:.3f}s")
        logger.info(f"  中位数响应时间: {stats['median']:.3f}s")
        logger.info(f"  P95响应时间: {stats['p95']:.3f}s")
        
        # 性能要求：平均响应时间应小于1秒
        assert stats['avg'] < 1.0, f"登录接口平均响应时间 {stats['avg']:.3f}s 超过1秒"
        
        client.close()
    
    @pytest.mark.performance
    @allure.title("订单列表接口响应时间测试")
    @allure.description("测试订单列表接口的响应时间性能")
    def test_order_list_response_time(self):
        """测试订单列表接口响应时间"""
        # 先登录获取token
        client = HttpClient(base_url="http://localhost:8088")
        login_resp = client.post("/api/user/login", json_data={
            "username": "test_user",
            "password": "Test@123456"
        })
        
        if login_resp.get("code") == 0:
            token = login_resp.get("data", {}).get("token")
            client.set_token(token)
        else:
            pytest.skip("登录失败，跳过测试")
        
        response_times = []
        
        # 执行10次查询
        for i in range(10):
            elapsed, response = self._measure_response_time(
                client, "GET", "/api/orders",
                params={"page": 1, "pageSize": 10}
            )
            response_times.append(elapsed)
            logger.info(f"订单列表第{i+1}次响应时间: {elapsed:.3f}s")
        
        stats = self._get_performance_stats(response_times)
        
        logger.info(f"\n订单列表接口性能统计:")
        logger.info(f"  最小响应时间: {stats['min']:.3f}s")
        logger.info(f"  最大响应时间: {stats['max']:.3f}s")
        logger.info(f"  平均响应时间: {stats['avg']:.3f}s")
        logger.info(f"  中位数响应时间: {stats['median']:.3f}s")
        logger.info(f"  P95响应时间: {stats['p95']:.3f}s")
        
        # 性能要求：平均响应时间应小于2秒
        assert stats['avg'] < 2.0, f"订单列表接口平均响应时间 {stats['avg']:.3f}s 超过2秒"
        
        client.close()
    
    @pytest.mark.performance
    @allure.title("票务列表接口响应时间测试")
    @allure.description("测试票务列表接口的响应时间性能")
    def test_ticket_list_response_time(self):
        """测试票务列表接口响应时间"""
        client = HttpClient(base_url="http://localhost:8088")
        
        # 测试多个端点
        endpoints = [
            ("/api/tickets", {"page": 1, "pageSize": 10}),
            ("/api/ticket/list", {"page": 1, "pageSize": 10}),
        ]
        
        for endpoint, params in endpoints:
            response_times = []
            
            for i in range(5):
                elapsed, response = self._measure_response_time(
                    client, "GET", endpoint, params=params
                )
                response_times.append(elapsed)
            
            stats = self._get_performance_stats(response_times)
            logger.info(f"\n{endpoint} 性能统计: 平均 {stats['avg']:.3f}s, P95 {stats['p95']:.3f}s")
            
            # 性能要求：平均响应时间应小于2秒
            if stats['avg'] > 0:
                assert stats['avg'] < 2.0, f"{endpoint} 平均响应时间 {stats['avg']:.3f}s 超过2秒"
        
        client.close()
    
    @pytest.mark.performance
    @allure.title("创建订单接口响应时间测试")
    @allure.description("测试创建订单接口的响应时间性能")
    def test_create_order_response_time(self):
        """测试创建订单接口响应时间"""
        client = HttpClient(base_url="http://localhost:8088")
        
        # 登录
        login_resp = client.post("/api/user/login", json_data={
            "username": "test_user",
            "password": "Test@123456"
        })
        
        if login_resp.get("code") == 0:
            client.set_token(login_resp.get("data", {}).get("token"))
        else:
            pytest.skip("登录失败")
        
        response_times = []
        
        # 执行5次创建订单
        for i in range(5):
            elapsed, response = self._measure_response_time(
                client, "POST", "/api/orders",
                json_data={
                    "ticketId": 1,
                    "sessionId": 1,
                    "quantity": 1,
                    "contactName": f"性能测试用户{i}",
                    "contactPhone": f"1380013{i:04d}"
                }
            )
            response_times.append(elapsed)
            logger.info(f"创建订单第{i+1}次响应时间: {elapsed:.3f}s")
        
        stats = self._get_performance_stats(response_times)
        
        logger.info(f"\n创建订单接口性能统计:")
        logger.info(f"  最小响应时间: {stats['min']:.3f}s")
        logger.info(f"  最大响应时间: {stats['max']:.3f}s")
        logger.info(f"  平均响应时间: {stats['avg']:.3f}s")
        logger.info(f"  P95响应时间: {stats['p95']:.3f}s")
        
        # 性能要求：平均响应时间应小于3秒
        if stats['avg'] > 0:
            assert stats['avg'] < 3.0, f"创建订单接口平均响应时间 {stats['avg']:.3f}s 超过3秒"
        
        client.close()


@allure.feature("性能测试")
@allure.story("并发处理能力测试")
class TestConcurrency:
    """并发处理能力测试"""
    
    @pytest.mark.performance
    @pytest.mark.concurrency
    @allure.title("登录接口并发测试")
    @allure.description("测试登录接口的并发处理能力")
    def test_login_concurrency(self):
        """测试登录接口并发"""
        def login_request(thread_id: int) -> Dict:
            client = HttpClient(base_url="http://localhost:8088")
            start_time = time.time()
            
            try:
                response = client.post("/api/user/login", json_data={
                    "username": "test_user",
                    "password": "Test@123456"
                })
                success = response.get("code") == 0
            except Exception as e:
                logger.error(f"线程{thread_id}登录异常: {e}")
                success = False
            
            elapsed = time.time() - start_time
            client.close()
            
            return {
                "thread_id": thread_id,
                "success": success,
                "elapsed": elapsed
            }
        
        # 执行10个并发请求
        num_threads = 10
        results = []
        
        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(login_request, i) for i in range(num_threads)]
            
            for future in as_completed(futures):
                results.append(future.result())
        
        # 统计结果
        success_count = sum(1 for r in results if r["success"])
        failed_count = num_threads - success_count
        response_times = [r["elapsed"] for r in results]
        
        logger.info(f"\n登录并发测试结果:")
        logger.info(f"  总请求数: {num_threads}")
        logger.info(f"  成功: {success_count}")
        logger.info(f"  失败: {failed_count}")
        logger.info(f"  平均响应时间: {statistics.mean(response_times):.3f}s")
        logger.info(f"  最大响应时间: {max(response_times):.3f}s")
        
        # 并发要求：成功率应大于90%
        success_rate = success_count / num_threads
        assert success_rate > 0.9, f"并发成功率 {success_rate:.1%} 低于90%"
    
    @pytest.mark.performance
    @pytest.mark.concurrency
    @allure.title("订单查询并发测试")
    @allure.description("测试订单查询接口的并发处理能力")
    def test_order_query_concurrency(self):
        """测试订单查询并发"""
        # 先登录获取token
        client = HttpClient(base_url="http://localhost:8088")
        login_resp = client.post("/api/user/login", json_data={
            "username": "test_user",
            "password": "Test@123456"
        })
        
        if login_resp.get("code") != 0:
            pytest.skip("登录失败")
        
        token = login_resp.get("data", {}).get("token")
        client.close()
        
        def query_request(thread_id: int) -> Dict:
            client = HttpClient(base_url="http://localhost:8088", token=token)
            start_time = time.time()
            
            try:
                response = client.get("/api/orders", params={"page": 1, "pageSize": 10})
                success = response.get("code") == 0
            except Exception as e:
                logger.error(f"线程{thread_id}查询异常: {e}")
                success = False
            
            elapsed = time.time() - start_time
            client.close()
            
            return {
                "thread_id": thread_id,
                "success": success,
                "elapsed": elapsed
            }
        
        # 执行20个并发请求
        num_threads = 20
        results = []
        
        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(query_request, i) for i in range(num_threads)]
            
            for future in as_completed(futures):
                results.append(future.result())
        
        # 统计结果
        success_count = sum(1 for r in results if r["success"])
        failed_count = num_threads - success_count
        response_times = [r["elapsed"] for r in results]
        
        logger.info(f"\n订单查询并发测试结果:")
        logger.info(f"  总请求数: {num_threads}")
        logger.info(f"  成功: {success_count}")
        logger.info(f"  失败: {failed_count}")
        logger.info(f"  平均响应时间: {statistics.mean(response_times):.3f}s")
        logger.info(f"  最大响应时间: {max(response_times):.3f}s")
        logger.info(f"  吞吐量: {num_threads / sum(response_times):.2f} 请求/秒")
        
        # 并发要求：成功率应大于95%
        success_rate = success_count / num_threads
        assert success_rate > 0.95, f"并发成功率 {success_rate:.1%} 低于95%"
    
    @pytest.mark.performance
    @pytest.mark.concurrency
    @allure.title("混合场景并发测试")
    @allure.description("测试多种接口混合并发的处理能力")
    def test_mixed_concurrency(self):
        """测试混合场景并发"""
        # 先登录获取token
        client = HttpClient(base_url="http://localhost:8088")
        login_resp = client.post("/api/user/login", json_data={
            "username": "test_user",
            "password": "Test@123456"
        })
        
        if login_resp.get("code") != 0:
            pytest.skip("登录失败")
        
        token = login_resp.get("data", {}).get("token")
        client.close()
        
        def mixed_request(thread_id: int) -> Dict:
            client = HttpClient(base_url="http://localhost:8088", token=token)
            start_time = time.time()
            
            try:
                # 根据线程ID选择不同的操作
                if thread_id % 3 == 0:
                    # 查询订单
                    response = client.get("/api/orders", params={"page": 1, "pageSize": 10})
                elif thread_id % 3 == 1:
                    # 查询票务
                    response = client.get("/api/tickets", params={"page": 1, "pageSize": 10})
                    if response.get("code") != 0:
                        response = client.get("/api/ticket/list", params={"page": 1, "pageSize": 10})
                else:
                    # 获取用户信息
                    response = client.get("/api/user/info")
                
                success = response.get("code") == 0
            except Exception as e:
                logger.error(f"线程{thread_id}请求异常: {e}")
                success = False
            
            elapsed = time.time() - start_time
            client.close()
            
            return {
                "thread_id": thread_id,
                "success": success,
                "elapsed": elapsed
            }
        
        # 执行30个混合并发请求
        num_threads = 30
        results = []
        
        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(mixed_request, i) for i in range(num_threads)]
            
            for future in as_completed(futures):
                results.append(future.result())
        
        # 统计结果
        success_count = sum(1 for r in results if r["success"])
        failed_count = num_threads - success_count
        response_times = [r["elapsed"] for r in results]
        
        logger.info(f"\n混合并发测试结果:")
        logger.info(f"  总请求数: {num_threads}")
        logger.info(f"  成功: {success_count}")
        logger.info(f"  失败: {failed_count}")
        logger.info(f"  平均响应时间: {statistics.mean(response_times):.3f}s")
        logger.info(f"  最大响应时间: {max(response_times):.3f}s")
        logger.info(f"  P95响应时间: {sorted(response_times)[int(len(response_times) * 0.95)]:.3f}s")
        logger.info(f"  吞吐量: {num_threads / sum(response_times):.2f} 请求/秒")
        
        # 并发要求：成功率应大于90%
        success_rate = success_count / num_threads
        assert success_rate > 0.9, f"并发成功率 {success_rate:.1%} 低于90%"


@allure.feature("性能测试")
@allure.story("压力测试")
class TestStress:
    """压力测试"""
    
    @pytest.mark.performance
    @pytest.mark.stress
    @allure.title("短时压力测试")
    @allure.description("对系统进行短时压力测试")
    def test_short_stress(self):
        """短时压力测试"""
        # 先登录获取token
        client = HttpClient(base_url="http://localhost:8088")
        login_resp = client.post("/api/user/login", json_data={
            "username": "test_user",
            "password": "Test@123456"
        })
        
        if login_resp.get("code") != 0:
            pytest.skip("登录失败")
        
        token = login_resp.get("data", {}).get("token")
        client.close()
        
        request_count = 0
        success_count = 0
        failed_count = 0
        start_time = time.time()
        
        def stress_request(thread_id: int) -> Dict:
            nonlocal request_count, success_count, failed_count
            
            client = HttpClient(base_url="http://localhost:8088", token=token)
            
            try:
                response = client.get("/api/orders", params={"page": 1, "pageSize": 10})
                request_count += 1
                
                if response.get("code") == 0:
                    success_count += 1
                    return True
                else:
                    failed_count += 1
                    return False
            except Exception:
                failed_count += 1
                return False
            finally:
                client.close()
        
        # 20个线程持续发送请求
        num_threads = 20
        duration = 10  # 持续10秒
        results = []
        
        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = []
            while time.time() - start_time < duration:
                future = executor.submit(stress_request, len(futures))
                futures.append(future)
                time.sleep(0.1)  # 控制请求速率
            
            for future in as_completed(futures):
                future.result()
        
        actual_duration = time.time() - start_time
        total_requests = success_count + failed_count
        
        logger.info(f"\n压力测试结果:")
        logger.info(f"  持续时间: {actual_duration:.2f}秒")
        logger.info(f"  总请求数: {total_requests}")
        logger.info(f"  成功: {success_count}")
        logger.info(f"  失败: {failed_count}")
        logger.info(f"  成功率: {success_count / total_requests * 100:.2f}%")
        logger.info(f"  平均QPS: {total_requests / actual_duration:.2f}")
        
        # 压力测试要求：成功率应大于85%
        if total_requests > 0:
            success_rate = success_count / total_requests
            assert success_rate > 0.85, f"压力测试成功率 {success_rate:.1%} 低于85%"


@allure.feature("性能测试")
@allure.story("数据库查询性能")
class TestDatabasePerformance:
    """数据库查询性能测试"""
    
    @pytest.mark.performance
    @pytest.mark.db
    @allure.title("订单列表数据库查询性能")
    @allure.description("测试订单列表查询的数据库性能")
    def test_order_list_db_performance(self):
        """测试订单列表数据库查询性能"""
        client = HttpClient(base_url="http://localhost:8088")
        
        # 登录
        login_resp = client.post("/api/user/login", json_data={
            "username": "test_user",
            "password": "Test@123456"
        })
        
        if login_resp.get("code") == 0:
            client.set_token(login_resp.get("data", {}).get("token"))
        else:
            pytest.skip("登录失败")
        
        # 测试不同分页大小的查询性能
        page_sizes = [10, 20, 50, 100]
        
        for page_size in page_sizes:
            start_time = time.time()
            
            response = client.get("/api/orders", params={"page": 1, "pageSize": page_size})
            
            elapsed = time.time() - start_time
            
            logger.info(f"分页大小={page_size}, 响应时间={elapsed:.3f}s")
            
            # 分页大小越大，响应时间可能越长，但不应超过5秒
            if response.get("code") == 0:
                assert elapsed < 5.0, f"分页大小={page_size}时响应时间 {elapsed:.3f}s 超过5秒"
        
        client.close()
    
    @pytest.mark.performance
    @pytest.mark.db
    @allure.title("订单详情数据库查询性能")
    @allure.description("测试订单详情查询的数据库性能")
    def test_order_detail_db_performance(self):
        """测试订单详情数据库查询性能"""
        client = HttpClient(base_url="http://localhost:8088")
        
        # 登录
        login_resp = client.post("/api/user/login", json_data={
            "username": "test_user",
            "password": "Test@123456"
        })
        
        if login_resp.get("code") == 0:
            client.set_token(login_resp.get("data", {}).get("token"))
        else:
            pytest.skip("登录失败")
        
        # 获取一个订单ID
        orders_resp = client.get("/api/orders", params={"page": 1, "pageSize": 1})
        
        if orders_resp.get("code") == 0:
            orders = orders_resp.get("data", {}).get("list", [])
            
            if orders:
                order_id = orders[0].get("orderId")
                
                # 测试详情查询
                start_time = time.time()
                detail_resp = client.get(f"/api/orders/{order_id}")
                elapsed = time.time() - start_time
                
                logger.info(f"订单详情查询响应时间={elapsed:.3f}s")
                
                # 单条详情查询应小于1秒
                assert elapsed < 1.0, f"订单详情查询响应时间 {elapsed:.3f}s 超过1秒"
        
        client.close()


# ==================== 性能测试配置 ====================

@pytest.fixture(scope="session", autouse=True)
def performance_test_setup():
    """性能测试前置设置"""
    logger.info("=" * 60)
    logger.info("开始性能测试")
    logger.info("=" * 60)
    
    yield
    
    logger.info("=" * 60)
    logger.info("性能测试完成")
    logger.info("=" * 60)
