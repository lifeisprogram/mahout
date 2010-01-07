/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.fpm.pfpgrowth.fpgrowth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/** {@link FrequentPatternMaxHeap} keeps top K Attributes in a TreeSet */
public final class FrequentPatternMaxHeap {

  private int count = 0;

  private Pattern least = null;

  private int maxSize = 0;

  private boolean subPatternCheck = false;

  private Map<Long, Set<Pattern>> patternIndex = null;

  private PriorityQueue<Pattern> queue = null;

  public FrequentPatternMaxHeap(int numResults, boolean subPatternCheck) {
    maxSize = numResults;
    queue = new PriorityQueue<Pattern>(maxSize);
    this.subPatternCheck = subPatternCheck;
    patternIndex = new HashMap<Long, Set<Pattern>>();
    for (Pattern p : queue) {
      Long index = Long.valueOf(p.support());
      Set<Pattern> patternList;
      if (patternIndex.containsKey(index) == false) {
        patternList = new HashSet<Pattern>();
        patternIndex.put(index, patternList);
      }
      patternList = patternIndex.get(index);
      patternList.add(p);

    }
  }

  public boolean addable(long support) {
    if (count < maxSize) {
      return true;
    }
    return least.support() <= support;
  }

  public PriorityQueue<Pattern> getHeap() {
    if (subPatternCheck) {
      PriorityQueue<Pattern> ret = new PriorityQueue<Pattern>(maxSize);
      for (Pattern p : queue) {

        if (patternIndex.get(p.support()).contains(p)) {
          ret.add(p);
        }
      }
      return ret;
    } else {
      return queue;
    }
  }

  public void addAll(FrequentPatternMaxHeap patterns, int attribute, long attributeSupport) {
    for (Pattern pattern : patterns.getHeap()) {
      long support = Math.min(attributeSupport, pattern.support());
      if (this.addable(support)) {
        pattern.add(attribute, support);
        this.insert(pattern);
      }
    }
  }

  public void insert(Pattern frequentPattern) {
    if (frequentPattern.length() == 0) {
      return;
      }
    
    if (count == maxSize) {
      if (frequentPattern.compareTo(least) > 0) {
        if (addPattern(frequentPattern)) {
          Pattern evictedItem = queue.poll();
          least = queue.peek();
          if (subPatternCheck) {
            patternIndex.get(evictedItem.support()).remove(evictedItem);
          }

        }
      }
    } else {
      if (addPattern(frequentPattern)) {
        count++;
        if (least != null) {
          if (least.compareTo(frequentPattern) < 0) {
            least = frequentPattern;
          }
        } else {
          least = frequentPattern;
        }
      }
    }
  }

  public int count() {
    return count;
  }

  public boolean isFull() {
    return count == maxSize;
  }

  public long leastSupport() {
    if (least == null) {
      return 0;
    }
    return least.support();
  }

  @Override
  public String toString() {
    return super.toString();
  }

  private boolean addPattern(Pattern frequentPattern) {
    if (subPatternCheck == false) {
      queue.add(frequentPattern);
      return true;
    } else {
      Long index = frequentPattern.support();
      if (patternIndex.containsKey(index)) {
        Set<Pattern> indexSet = patternIndex.get(index);
        boolean replace = false;
        Pattern replacablePattern = null;
        for (Pattern p : indexSet) {
          if (frequentPattern.isSubPatternOf(p)) {
            return false;
          } else if (p.isSubPatternOf(frequentPattern)) {
            replace = true;
            replacablePattern = p;
            break;
          }
        }
        if (replace) {
          indexSet.remove(replacablePattern);
          if (indexSet.contains(frequentPattern) == false && queue.add(frequentPattern)) {
            
            indexSet.add(frequentPattern);
          }
          return false;
        }
        queue.add(frequentPattern);
        indexSet.add(frequentPattern);
        return true;
      } else {
        queue.add(frequentPattern);
        Set<Pattern> patternList;
        if (patternIndex.containsKey(index) == false) {
          patternList = new HashSet<Pattern>();
          patternIndex.put(index, patternList);
        }
        patternList = patternIndex.get(index);
        patternList.add(frequentPattern);

        return true;
      }
    }
  }
}
