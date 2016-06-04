// goseumdochi:  experiments with incarnation
// Copyright 2016 John V. Sichi
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.goseumdochi.vision

import scala.collection.mutable

// adapted from DisjointSet.scala in https://github.com/pathikrit/scalgos
// thanks!
class DisjointSet[A]
{
  import DisjointSet.Node

  private[this] val parent = mutable.Map.empty[A, Node[A]]

  def toNode(x: A) =
  {
    assume(contains(x))
    parent(x)
  }

  def contains(x: A) = parent contains x

  def +=(x: A) =
  {
    assume(!contains(x))
    parent(x) = new Node(x)
  }

  def union(x: A, y: A) =
  {
    val (xRoot, yRoot) = (toNode(x).root, toNode(y).root)
    if (xRoot != yRoot) {
      // change the root of the shorter/less-depth one
      if (xRoot.rank < yRoot.rank) {
        xRoot.parent = yRoot
      } else if (xRoot.rank > yRoot.rank) {
        yRoot.parent = xRoot
      } else {
        yRoot.parent = xRoot
        xRoot.rank += 1   // else if there is tie, increment
      }
    }
  }

  def apply(x: A) = toNode(x).root.entry

  def sets = parent.keys.groupBy({toNode(_).root.entry}).values
}

object DisjointSet
{
  private[DisjointSet] class Node[A](val entry: A)
  {
    var (parent, rank) = (this, 0)

    def root: Node[A] = {
      if (parent != this) {
        parent = parent.root     // path compression
      }
      parent
    }
  }

  def empty[A] = new DisjointSet[A]

  def apply[A](elements: A*) =
  {
    val d = empty[A]
    elements foreach {e => d += e}
    d
  }
}
