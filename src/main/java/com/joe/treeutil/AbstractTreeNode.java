package com.joe.treeutil;

import java.io.Serializable;

/**
 * @author HJY
 * @date 2020/5/10
 */
public abstract class AbstractTreeNode implements TreeNode, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 按照order由小到大排序
     * @param o
     * @return
     */
    @Override
    public int compareTo(Object o) {
        if (TreeNode.class.isInstance(o)) {
            Integer nodeOrder = ((TreeNode) o).getOrder();
            if (nodeOrder == null) {
                return -1;
            } else {
                if (this.getOrder() == null) {
                    return -1;
                } else {
                    return this.getOrder() - nodeOrder;
                }
            }
        }
        return 0;
    }
}
