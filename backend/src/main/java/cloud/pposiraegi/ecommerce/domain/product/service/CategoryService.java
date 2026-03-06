package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.dto.CategoryDto;
import cloud.pposiraegi.ecommerce.domain.product.entity.Category;
import cloud.pposiraegi.ecommerce.domain.product.repository.CategoryRepository;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final TsidFactory tsidFactory;

    @Transactional
    public CategoryDto.CategoryCreateResponse createCategory(CategoryDto.CategoryCreateRequest request) {
        int depth = 1;

        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            depth = parent.getDepth() + 1;
        }

        Category category = Category.builder()
                .id(tsidFactory.create().toLong())
                .parentId(request.parentId())
                .name(request.name())
                .depth(depth)
                .displayOrder(request.displayOrder())
                .build();

        Category saved = categoryRepository.save(category);

        return CategoryDto.CategoryCreateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto.CategoryResponse> getCategoryTrees() {
        List<Category> allCategories = categoryRepository.findAllByOrderByDisplayOrderAsc();

        Map<Long, List<CategoryDto.CategoryResponse>> groupingByParent = allCategories.stream()
                .filter(category -> category.getParentId() != null)
                .map(CategoryDto.CategoryResponse::from)
                .collect(Collectors.groupingBy(CategoryDto.CategoryResponse::parentId));

        List<CategoryDto.CategoryResponse> rootCategories = allCategories.stream()
                .filter(category -> category.getParentId() == null)
                .map(CategoryDto.CategoryResponse::from)
                .toList();

        assembleTree(rootCategories, groupingByParent);

        return rootCategories;
    }

    private void assembleTree(List<CategoryDto.CategoryResponse> parents, Map<Long, List<CategoryDto.CategoryResponse>> groupingByParent) {
        for (CategoryDto.CategoryResponse parent : parents) {
            List<CategoryDto.CategoryResponse> children = groupingByParent.getOrDefault(parent.id(), List.of());
            parent.children().addAll(children);

            assembleTree(children, groupingByParent);

        }
    }
}
