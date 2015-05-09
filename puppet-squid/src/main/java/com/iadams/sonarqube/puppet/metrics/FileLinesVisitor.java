/**
 * Sonar Puppet Plugin
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Iain Adams
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
package com.iadams.sonarqube.puppet.metrics;

import com.google.common.collect.Sets;
import com.iadams.sonarqube.puppet.api.PuppetMetric;
import com.iadams.sonarqube.puppet.api.PuppetTokenType;
import com.sonar.sslr.api.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.squidbridge.SquidAstVisitor;

import java.util.List;
import java.util.Set;

/**
 * Created by iwarapter
 *
 * Visitor that computes {@link CoreMetrics#NCLOC_DATA_KEY} and {@link CoreMetrics#COMMENT_LINES_DATA_KEY} metrics used by the DevCockpit.
 */
public class FileLinesVisitor extends SquidAstVisitor<Grammar> implements AstAndTokenVisitor {

	private final Project project;
	private final FileLinesContextFactory fileLinesContextFactory;

	private final Set<Integer> linesOfCode = Sets.newHashSet();
	private final Set<Integer> linesOfComments = Sets.newHashSet();

	public FileLinesVisitor(Project project, FileLinesContextFactory fileLinesContextFactory) {
		this.project = project;
		this.fileLinesContextFactory = fileLinesContextFactory;
	}

	public void visitToken(Token token) {
		if (token.getType().equals(GenericTokenType.EOF)) {
			return;
		}

		if (token.getType() != PuppetTokenType.DEDENT && token.getType() != PuppetTokenType.INDENT && token.getType() != PuppetTokenType.NEWLINE) {
      /* Handle all the lines of the token */
			String[] tokenLines = token.getValue().split("\n", -1);
			for (int line = token.getLine(); line < token.getLine() + tokenLines.length; line++) {
				linesOfCode.add(line);
			}
		}

		List<Trivia> trivias = token.getTrivia();
		for (Trivia trivia : trivias) {
			if (trivia.isComment()) {
				linesOfComments.add(trivia.getToken().getLine());
			}
		}
	}

	@Override
	public void leaveFile(AstNode astNode) {
		File sonarFile = File.fromIOFile(getContext().getFile(), project);
		FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(sonarFile);

		int fileLength = getContext().peekSourceCode().getInt(PuppetMetric.LINES);
		for (int line = 1; line <= fileLength; line++) {
			fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, linesOfCode.contains(line) ? 1 : 0);
			fileLinesContext.setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, line, linesOfComments.contains(line) ? 1 : 0);
		}
		fileLinesContext.save();

		linesOfCode.clear();
		linesOfComments.clear();
	}
}
