"""买家前台（shop）的页面对象：票务详情页。"""

from playwright.sync_api import Page, expect


class TicketDetailPage:
    """票务详情页：/shop/tickets/{id}，任何人可见，不需要登录。"""

    def __init__(self, page: Page, base_url: str) -> None:
        self.page = page
        self.base_url = base_url

    def open(self, ticket_id: int) -> "TicketDetailPage":
        self.page.goto(f"{self.base_url}/shop/tickets/{ticket_id}")
        return self

    def expect_title(self, name: str) -> None:
        """主标题断言。详情页主标题是 h1；侧边栏还有一个同名 h2，
        不带 level 会撞 strict mode——这个页面知识写在 POM 里，
        用例不需要知道。"""
        expect(
            self.page.get_by_role("heading", level=1, name=name, exact=True)
        ).to_be_visible()

    def expect_comment(self, content: str) -> None:
        """断言某条评论出现在评论区。"""
        expect(self.page.get_by_text(content)).to_be_visible()
