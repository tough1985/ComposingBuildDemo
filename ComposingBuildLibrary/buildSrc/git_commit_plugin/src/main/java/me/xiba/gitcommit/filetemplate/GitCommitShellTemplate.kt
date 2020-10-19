package me.xiba.gitcommit.filetemplate

/**
 * @Author:liukun
 * @Date: 2020-10-17 14:11
 * @Description: gitcommit.sh模板文本
 */
class GitCommitShellTemplate {

    companion object {
        const val GIT_COMMIT_SHELL_TEMPLATE = "#!/bin/bash\n" +
                "\n" +
                "# 传递参数\n" +
                "\n" +
                "if [ ! \$1 ]\n" +
                "then\n" +
                "    echo \"########## 请输入提交信息 ##########\"\n" +
                "    exit 1;\n" +
                "fi\n" +
                "\n" +
                "echo \"########## 开始提交 ##########\"\n" +
                "\n" +
                "echo \"commitMessage: \$1\"\n" +
                "\n" +
                "git add -A\n" +
                "\n" +
                "# 获取git status的结果\n" +
                "statusResult=`git status`\n" +
                "\n" +
                "# 如果返回内容包含'nothing to commit'说明没有要返回的内容，直接返回\n" +
                "if [[ \$statusResult == *\"nothing to commit\"* ]]\n" +
                "then\n" +
                "  echo \"########## 没有需要提交的内容 ##########\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "\n" +
                "echo \"########## 请输入提交信息 ##########\"\n" +
                "\n" +
                "git commit -m \"\$1\"\n" +
                "\n" +
                "if [ \$? -ne 0 ]\n" +
                "then\n" +
                "  echo \"git commit 错误\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "\n" +
                "git fetch\n" +
                "if [ \$? -ne 0 ]\n" +
                "then\n" +
                "  echo \"git fetch 错误\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "\n" +
                "git rebase\n" +
                "if [ \$? -ne 0 ]\n" +
                "then\n" +
                "  echo \"git rebase 错误\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "\n" +
                "git push -u origin\n" +
                "if [ \$? -ne 0 ]\n" +
                "then\n" +
                "  echo \"git push 错误\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "\n" +
                "echo \"########## 提交结束 ##########\""
    }
}