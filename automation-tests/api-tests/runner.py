"""
测试运行器

提供测试执行的入口，支持不同的运行模式
"""

import sys
import argparse
from pathlib import Path

import pytest


# 项目根目录
PROJECT_ROOT = Path(__file__).parent
TEST_DIR = PROJECT_ROOT / "api-tests" / "tests"


def run_tests(
    markers: str = None,
    keywords: str = None,
    nodes: str = None,
    html_report: bool = True,
    allure_report: bool = True,
    parallel: bool = False,
    max_workers: int = 4,
    verbose: int = 1,
    stop_on_fail: bool = False,
    last_failed: bool = False
):
    """
    运行测试
    
    Args:
        markers: 运行指定标记的测试，如 "user or ticket"
        keywords: 按关键字过滤测试
        nodes: 运行指定节点（测试类/函数），如 "test_user.py::TestUserLogin"
        html_report: 是否生成HTML报告
        allure_report: 是否生成Allure报告
        parallel: 是否并行运行
        max_workers: 并行线程数
        verbose: 输出详细程度 0-3
        stop_on_fail: 遇到失败是否停止
        last_failed: 只运行上次失败的测试
    """
    # 构建pytest参数
    args = [str(TEST_DIR)]
    
    # 输出控制
    args.append(f"-v" * verbose)
    
    # 标记过滤
    if markers:
        args.append(f"-m {markers}")
    
    # 关键字过滤
    if keywords:
        args.append(f"-k {keywords}")
    
    # 指定节点
    if nodes:
        args.append(nodes)
    
    # 失败时停止
    if stop_on_fail:
        args.append("-x")
    
    # 只运行上次失败
    if last_failed:
        args.append("--lf")
    
    # HTML报告
    if html_report:
        report_dir = PROJECT_ROOT / "report"
        report_dir.mkdir(exist_ok=True)
        html_file = report_dir / "test_report.html"
        args.append(f"--html={html_file}")
        args.append("--self-contained-html")
    
    # Allure报告
    if allure_report:
        allure_dir = PROJECT_ROOT / "report" / "allure-results"
        args.append(f"--alluredir={allure_dir}")
    
    # 并行执行
    if parallel:
        args.append(f"-n {max_workers}")
    
    # 运行pytest
    exit_code = pytest.main(args)
    return exit_code


def run_smoke_tests():
    """运行冒烟测试"""
    print("=" * 60)
    print("Running Smoke Tests")
    print("=" * 60)
    return run_tests(markers="smoke")


def run_user_tests():
    """运行用户模块测试"""
    print("=" * 60)
    print("Running User Module Tests")
    print("=" * 60)
    return run_tests(markers="user")


def run_ticket_tests():
    """运行票务模块测试"""
    print("=" * 60)
    print("Running Ticket Module Tests")
    print("=" * 60)
    return run_tests(markers="ticket")


def run_order_tests():
    """运行订单模块测试"""
    print("=" * 60)
    print("Running Order Module Tests")
    print("=" * 60)
    return run_tests(markers="order")


def run_regression_tests():
    """运行回归测试"""
    print("=" * 60)
    print("Running Regression Tests")
    print("=" * 60)
    return run_tests(markers="regression")


def generate_allure_report():
    """生成Allure报告"""
    allure_dir = PROJECT_ROOT / "report" / "allure-results"
    output_dir = PROJECT_ROOT / "report" / "allure-report"
    
    import subprocess
    cmd = ["allure", "generate", str(allure_dir), "-o", str(output_dir), "--clean"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    
    if result.returncode == 0:
        print(f"Allure report generated: {output_dir}")
        # 自动打开报告
        subprocess.run(["allure", "open", str(output_dir)])
    else:
        print(f"Failed to generate report: {result.stderr}")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="接口自动化测试运行器")
    
    # 运行模式
    parser.add_argument(
        "--mode",
        choices=["all", "smoke", "user", "ticket", "order", "cart", "coupon", "regression"],
        default="all",
        help="运行模式"
    )
    
    # 标记过滤
    parser.add_argument("-m", "--markers", help="按标记过滤，如 'user and smoke'")
    
    # 关键字过滤
    parser.add_argument("-k", "--keywords", help="按关键字过滤")
    
    # 节点选择
    parser.add_argument("-n", "--nodes", help="运行指定节点，如 test_user.py::TestUserLogin")
    
    # 报告选项
    parser.add_argument("--no-html", action="store_true", help="不生成HTML报告")
    parser.add_argument("--no-allure", action="store_true", help="不生成Allure报告")
    
    # 并行
    parser.add_argument("-p", "--parallel", action="store_true", help="并行执行")
    parser.add_argument("--workers", type=int, default=4, help="并行线程数")
    
    # 详细输出
    parser.add_argument("-v", "--verbose", action="count", default=1, help="输出详细程度")
    
    # 失败时停止
    parser.add_argument("-x", "--stop-on-fail", action="store_true", help="遇到失败停止")
    
    # 只运行失败
    parser.add_argument("--lf", "--last-failed", action="store_true", help="只运行上次失败的测试")
    
    # 生成报告
    parser.add_argument("--report", action="store_true", help="生成Allure报告")
    
    args = parser.parse_args()
    
    # 根据模式确定标记
    markers = args.markers
    if not markers:
        mode_markers = {
            "all": None,
            "smoke": "smoke",
            "user": "user",
            "ticket": "ticket",
            "order": "order",
            "cart": "cart",
            "coupon": "coupon",
            "regression": "regression"
        }
        markers = mode_markers.get(args.mode)
    
    # 运行测试
    exit_code = run_tests(
        markers=markers,
        keywords=args.keywords,
        nodes=args.nodes,
        html_report=not args.no_html,
        allure_report=not args.no_allure,
        parallel=args.parallel,
        max_workers=args.workers,
        verbose=args.verbose,
        stop_on_fail=args.stop_on_fail,
        last_failed=args.last_failed
    )
    
    # 生成报告
    if args.report and not args.no_allure:
        generate_allure_report()
    
    # 返回退出码
    sys.exit(exit_code)


if __name__ == "__main__":
    main()