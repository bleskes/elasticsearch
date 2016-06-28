/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.data.extractors.elasticsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Range;

/**
 * <p>A red-black tree that allows storing values associated to
 * an interval and searching efficiently for the values that are
 * overlapping with a given interval.
 *
 * <p>The implementation is based upon the interval tree, an augmented
 * red-black tree, as described in the Introduction to Algorithms book written by CLRS.
 * Guava's {@link Range} class is used to represent intervals. That facilitates
 * with dealing with ranges whose bounds can be open/closed.
 *
 * <p>Note that duplicate intervals are allowed.
 *
 * @param <K> A comparable type that is the type of the interval bounds.
 * @param <V> The type of the stored values.
 */
public class IntervalTree<K extends Comparable<K>, V>
{
    private Node m_Root;

    /**
     * Constructs an empty tree.
     */
    public IntervalTree()
    {
        m_Root = null;
    }

    /**
     * Clears the tree.
     */
    public void clear()
    {
        m_Root = null;
    }

    /**
     * Returns <tt>true</tt> if this tree is empty.
     *
     * @return <tt>true</tt> if this tree is empty.
     */
    public boolean isEmpty()
    {
        return m_Root == null;
    }

    /**
     * Inserts a value associated with an interval into the appropriate
     * location in the tree. The tree gets re-balanced to ensure the validity
     * of the properties of an interval tree.
     *
     * @param interval the key of the new node
     * @param value the value of the new node
     */
    public void put(Range<K> interval, V value)
    {
        Objects.requireNonNull(interval);
        Objects.requireNonNull(value);
        Node insertedNode = new Node(interval, value);
        Node parentNode = null;
        Node currentNode = m_Root;
        while (currentNode != null)
        {
            parentNode = currentNode;
            currentNode = lowerBoundCompare(interval, currentNode.interval) < 0 ?
                    currentNode.left : currentNode.right;
        }

        insertedNode.parent = parentNode;
        if (parentNode == null)
        {
            m_Root = insertedNode;
        }
        else if (lowerBoundCompare(interval, parentNode.interval) < 0)
        {
            parentNode.left = insertedNode;
        }
        else
        {
            parentNode.right = insertedNode;
        }

        updateAncestorsMax(insertedNode);
        insertionRebalance(insertedNode);
    }

    private int lowerBoundCompare(Range<K> r1, Range<K> r2)
    {
        return r1.lowerEndpoint().compareTo(r2.lowerEndpoint());
    }

    private void updateAncestorsMax(Node node)
    {
        Node current = node;
        Node parent = current.parent;
        while (parent != null && parent.max.compareTo(current.max) < 0)
        {
            parent.max = max(parent.max, current.max);
            current = parent;
            parent = current.parent;
        }
    }

    /**
     * Implementation of CLRS RB-INSERT-FIXUP
     *
     * @param node the newly inserted node.
     */
    private void insertionRebalance(Node node)
    {
        while (node.parent != null && node.parent.colour == RED)
        {
            if (node.parent.isLeftChild())
            {
                Node y = node.parent.parent.right;
                if (y != null && y.colour == RED)
                {
                    node.parent.colour = BLACK;
                    y.colour = BLACK;
                    node.parent.parent.colour = RED;
                    node = node.parent.parent;
                }
                else
                {
                    if (node.isRightChild())
                    {
                        node = node.parent;
                        leftRotate(node);
                    }
                    node.parent.colour = BLACK;
                    node.parent.parent.colour = RED;
                    rightRotate(node.parent.parent);
                }
            }
            else
            {
                Node y = node.parent.parent.left;
                if (y != null && y.colour == RED)
                {
                    node.parent.colour = BLACK;
                    y.colour = BLACK;
                    node.parent.parent.colour = RED;
                    node = node.parent.parent;
                }
                else
                {
                    if (node.isLeftChild())
                    {
                        node = node.parent;
                        rightRotate(node);
                    }
                    node.parent.colour = BLACK;
                    node.parent.parent.colour = RED;
                    leftRotate(node.parent.parent);
                }
            }
        }
        m_Root.colour = BLACK;
    }

    /**
     * Rotates x into its right child, i.e. y.
     * @param x the node to rotate
     */
    void leftRotate(Node x)
    {
        Node y = x.right;
        x.right = y.left;
        if (y.left != null)
        {
            y.left.parent = x;
        }
        y.left = x;
        tradeParents(x, y);
        recalculateMax(x, y);
    }

    /**
     * Rotates x into its left child, i.e. y.
     * @param x the node to rotate
     */
    void rightRotate(Node x)
    {
        Node y = x.left;
        x.left = y.right;
        if (y.right != null)
        {
            y.right.parent = x;
        }
        y.right = x;
        tradeParents(x, y);
        recalculateMax(x, y);
    }

    private void tradeParents(Node parent, Node child)
    {
        child.parent = parent.parent;
        if (parent.parent == null)
        {
            m_Root = child;
        }
        else if (parent == parent.parent.left)
        {
            parent.parent.left = child;
        }
        else
        {
            parent.parent.right = child;
        }
        parent.parent = child;
    }

    private void recalculateMax(Node oldTopNode, Node newTopNode)
    {
        newTopNode.max = oldTopNode.max;
        oldTopNode.max = oldTopNode.interval.upperEndpoint();
        if (oldTopNode.left != null)
        {
            oldTopNode.max = max(oldTopNode.max, oldTopNode.left.max);
        }
        if (oldTopNode.right != null)
        {
            oldTopNode.max = max(oldTopNode.max, oldTopNode.right.max);
        }
    }

    private K max(K left, K right)
    {
        return left.compareTo(right) > 0 ? left : right;
    }

    /**
     * <p>Searches and returns a list containing the values that are
     * associated to an interval that intersects the given {@code interval}.
     *
     * <p>For example, given the tree contains:
     * [0..4] -> 1, [1..1] -> 2, [1..5] -> 3, [1..3] -> 4, [2..4] -> 5, [5..9] -> 6,
     * the intersecting values for interval [2..5) will be [1, 3, 4, 5].
     *
     * @param interval the interval for which intersecting values will be returned
     * @return the values that intersect the given {@code interval}
     */
    public List<V> getIntersectingValues(Range<K> interval)
    {
        List<V> result = new ArrayList<>();
        if (m_Root != null)
        {
            getIntersectingValues(interval, m_Root, result);
        }
        return result;
    }

    private void getIntersectingValues(Range<K> interval, Node node, List<V> result)
    {
        if (isIntersecting(interval, node.interval))
        {
            result.add(node.value);
        }
        if (node.left != null && node.left.max.compareTo(interval.lowerEndpoint()) >= 0)
        {
            getIntersectingValues(interval, node.left, result);
        }
        if (node.right != null
                && node.interval.lowerEndpoint().compareTo(interval.upperEndpoint()) <= 0
                && node.right.max.compareTo(interval.lowerEndpoint()) >= 0)
        {
            getIntersectingValues(interval, node.right, result);
        }
    }

    private boolean isIntersecting(Range<K> left, Range<K> right)
    {
        if (left.isConnected(right))
        {
            return !left.intersection(right).isEmpty();
        }
        return false;
    }

    /**
     * <p>Returns the height of this tree.
     *
     * <p>In particular, the node-count height is calculated. Thus, an
     * empty tree's height is 0 and a single node tree's height is 1.
     *
     * @return the height of this tree.
     */
    public int height()
    {
        return height(m_Root);
    }

    private int height(Node node)
    {
        return node == null ? 0 : Math.max(height(node.left), height(node.right)) + 1;
    }

    private static final boolean RED = false;
    private static final boolean BLACK = true;

    private class Node
    {
        Range<K> interval;
        V value;
        K max;
        boolean colour;
        Node parent;
        Node left;
        Node right;

        Node(Range<K> interval, V value)
        {
            this.interval = interval;
            this.value = value;
            this.max = interval.upperEndpoint();
            this.colour = RED;
        }

        private boolean isLeftChild()
        {
            return parent != null && parent.left == this;
        }

        private boolean isRightChild()
        {
            return parent != null && parent.right == this;
        }
    }
}
