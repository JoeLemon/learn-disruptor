package com.joe.treeutil;

import java.util.List;

/**
 * 树结构接口
 * @author HJY
 * @date 2020/4/2
 */
public interface TreeNode extends Comparable {

    Long getId();

    Long getParentId();

    Integer getOrder();

    void setChildren(List<TreeNode> treeNodes);

}
