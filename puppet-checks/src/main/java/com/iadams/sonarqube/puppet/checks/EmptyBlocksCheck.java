/*
 * SonarQube Puppet Plugin
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Iain Adams and David RACODON
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.iadams.sonarqube.puppet.checks;

import com.iadams.sonarqube.puppet.PuppetCheckVisitor;
import com.iadams.sonarqube.puppet.api.PuppetGrammar;
import com.iadams.sonarqube.puppet.api.PuppetPunctuator;
import com.sonar.sslr.api.AstNode;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "EmptyBlocks",
  name = "Empty blocks of code should be removed",
  priority = Priority.MAJOR,
  tags = {Tags.PITFALL})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class EmptyBlocksCheck extends PuppetCheckVisitor {

  @Override
  public void init() {
    subscribeTo(
      PuppetGrammar.CLASSDEF,
      PuppetGrammar.DEFINITION,

      PuppetGrammar.RESOURCE,
      PuppetGrammar.RESOURCE_OVERRIDE,

      PuppetGrammar.CASE_MATCHER,
      PuppetGrammar.IF_STMT,
      PuppetGrammar.ELSIF_STMT,
      PuppetGrammar.ELSE_STMT,
      PuppetGrammar.UNLESS_STMT);
  }

  @Override
  public void visitNode(AstNode node) {
    checkClassesAndDefines(node);
    checkResources(node);
    checkConditionalStatements(node);
  }

  private void checkClassesAndDefines(AstNode node) {
    if (node.is(PuppetGrammar.CLASSDEF, PuppetGrammar.DEFINITION)) {
      if (node.getFirstChild(PuppetGrammar.ARGUMENTS) == null
        && node.getFirstChild(PuppetGrammar.CLASSNAME).getNextAstNode().is(PuppetPunctuator.LPAREN)) {
        addIssue(node, this, "Remove this empty argument list.");
      }
      if (node.getFirstChild(PuppetGrammar.STATEMENT) == null) {
        String nodeType = node.is(PuppetGrammar.CLASSDEF) ? "class" : "define";
        addIssue(node, this, "Remove this empty " + nodeType + ".");
      }
    }
  }

  private void checkResources(AstNode node) {
    if (node.is(PuppetGrammar.RESOURCE)
      && node.getFirstChild(PuppetGrammar.RESOURCE_INST) == null
      && node.getFirstChild(PuppetGrammar.PARAMS).getChildren(PuppetGrammar.PARAM).isEmpty()) {
      addIssue(node, this, "Remove this empty resource default statement.");
    } else if (node.is(PuppetGrammar.RESOURCE_OVERRIDE)
      && node.getFirstChild(PuppetGrammar.ANY_PARAMS).getChildren(PuppetGrammar.PARAM, PuppetGrammar.ADD_PARAM).isEmpty()) {
      addIssue(node, this, "Remove this empty resource override.");
    }
  }

  private void checkConditionalStatements(AstNode node) {
    if (node.getFirstChild(PuppetGrammar.STATEMENT) == null) {
      if (node.is(PuppetGrammar.IF_STMT)) {
        addIssue(node, this, "Remove this empty \"if\" statement.");
      } else if (node.is(PuppetGrammar.UNLESS_STMT)) {
        addIssue(node, this, "Remove this empty \"unless\" statement.");
      } else if (!hasTrivia(node)) {
        if (node.is(PuppetGrammar.CASE_MATCHER)) {
          addIssue(node, this, "Remove this empty \"case\" matcher or add a comment to explain why it is empty.");
        } else if (node.is(PuppetGrammar.ELSIF_STMT)) {
          addIssue(node, this, "Remove this empty \"elsif\" statement or add a comment to explain why it is empty.");
        } else if (node.is(PuppetGrammar.ELSE_STMT)) {
          addIssue(node, this, "Remove this empty \"else\" statement or add a comment to explain why it is empty.");
        }
      }
    }
  }

  private static boolean hasTrivia(AstNode node) {
    if (node.getToken().hasTrivia()) {
      return true;
    }
    for (AstNode childNode : node.getChildren()) {
      if (childNode.getToken().hasTrivia()) {
        return true;
      }
    }
    return false;
  }

}
