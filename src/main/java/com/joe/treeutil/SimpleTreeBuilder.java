package com.joe.treeutil;

import lombok.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * 简单的树结构构建器
 * 适用于小数据量
 *
 * @author HJY
 * @date 2020/4/2
 */
public class SimpleTreeBuilder {

    /**
     * 根据List<TreeNode>构建树结构
     * @param treeNodeList
     * @param singleTreeCheck 是否要求构建为一棵树，当要求构建为一棵树时，构建结束剩余非根节点，抛出ServiceException异常
     * @return
     * @throws ServiceException
     */
    public static List<TreeNode> build(@NonNull List<? extends TreeNode> treeNodeList, boolean singleTreeCheck) throws Exception {
        Map<Long, List<TreeNode>> treeNodeMap = treeNodeList.stream()
                .sorted()
                .collect(groupingBy(node -> node.getParentId(), Collectors.toList()));
        Iterator<Map.Entry<Long, List<TreeNode>>> iterator = treeNodeMap.entrySet().iterator();
        Long rootkey = null;
        while (iterator.hasNext()) {
            Map.Entry<Long, List<TreeNode>> entryItem = iterator.next();
            Long key = entryItem.getKey();
            if (key == null) {
                continue;
            }
            boolean foundParentItem = false;
            for (TreeNode treeNode : treeNodeList) {
                if (key.equals(treeNode.getId())) {
                    foundParentItem = true;
                    treeNode.setChildren(entryItem.getValue());
                    iterator.remove();
                    break;
                }
            }
            if (!foundParentItem) {
                rootkey = entryItem.getKey();
            }
        }
        if (singleTreeCheck) {
            if (treeNodeMap.size() > 1) {
                throw new Exception("构建树失败");
            } else {
                return treeNodeMap.get(rootkey);
            }
        } else {
            List<TreeNode> result = null;
            for (Map.Entry<Long, List<TreeNode>> entry : treeNodeMap.entrySet()) {
                if (result == null) {
                    result = entry.getValue();
                } else {
                    result.addAll(entry.getValue());
                }
            }
            return result;
        }
    }
}
