package com.ticket.app.service.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.entity.Category;
import com.ticket.app.entity.Ticket;
import com.ticket.app.repository.ticket.CategoryRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分类服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;

    @Override
    public List<?> getCategories() {
        log.info("获取所有票务分类");

        // 查询票务分类(type=1)
        List<Category> categories = categoryRepository.selectCategories(Category.TYPE_TICKET);

        // 获取每个分类的票务数量
        Map<Integer, Long> countMap = getTicketCountMap();

        List<CategoryVO> voList = categories.stream()
                .map(category -> {
                    CategoryVO vo = new CategoryVO();
                    vo.setId(category.getId());
                    vo.setName(category.getName());
                    vo.setIcon(category.getIcon());
                    vo.setCount(countMap.getOrDefault(category.getId(), 0L).intValue());
                    return vo;
                })
                .collect(Collectors.toList());

        return voList;
    }

    @Override
    public List<?> getCategoriesWithCount() {
        return getCategories();
    }

    /**
     * 获取每个分类的票务数量映射
     */
    private Map<Integer, Long> getTicketCountMap() {
        // 查询状态为售票中的票务数量
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getStatus, 2); // 售票中
        wrapper.isNull(Ticket::getDeletedAt);
        
        List<Ticket> tickets = ticketRepository.selectList(wrapper);
        
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        Ticket::getCategoryId,
                        Collectors.counting()
                ));
    }

    /**
     * 分类VO
     */
    public static class CategoryVO {
        private Integer id;
        private String name;
        private String icon;
        private Integer count;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}