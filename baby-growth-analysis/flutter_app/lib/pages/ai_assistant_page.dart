/// AI 助手页面 - 聊天界面
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../providers/app_state.dart';

class AiAssistantPage extends StatefulWidget {
  const AiAssistantPage({super.key});

  @override
  State<AiAssistantPage> createState() => _AiAssistantPageState();
}

class _AiAssistantPageState extends State<AiAssistantPage> {
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  bool _isTyping = false;

  final List<String> quickPrompts = [
    '帮我写一篇宝宝今天的日记',
    '给宝宝讲一个温馨的睡前故事',
    '宝宝接种疫苗有哪些注意事项？',
    '根据宝宝月龄给我一些早教学习建议',
    '宝宝最近睡眠不太好怎么办？',
  ];

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _sendMessage() async {
    if (_controller.text.trim().isEmpty || _isTyping) return;

    final message = _controller.text.trim();
    _controller.clear();

    setState(() => _isTyping = true);

    final answer = await context.read<AppState>().sendMessage(message);

    if (!mounted) return;
    setState(() => _isTyping = false);

    // Auto scroll
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            // Header
            Padding(
              padding: const EdgeInsets.all(AppTheme.spacingLg),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: const BoxDecoration(
                        color: AppTheme.aiColor, shape: BoxShape.circle),
                    child: const Icon(Icons.auto_awesome,
                        color: Colors.white, size: 20),
                  ),
                  const SizedBox(width: AppTheme.spacingMd),
                  Text('AI 助手',
                      style: Theme.of(context).textTheme.headlineMedium),
                  const Spacer(),
                  IconButton(
                    icon: const Icon(Icons.delete_outline),
                    onPressed: () =>
                        context.read<AppState>().clearChatHistory(),
                    tooltip: '清空对话',
                  ),
                ],
              ),
            ),

            // Chat Area
            Expanded(
              child: Consumer<AppState>(
                builder: (context, state, _) {
                  final messages = state.chatHistory;

                  if (messages.isEmpty && !_isTyping) {
                    return SingleChildScrollView(
                      padding: const EdgeInsets.all(AppTheme.spacingLg),
                      child: Column(
                        children: [
                          const SizedBox(height: 40),
                          Icon(Icons.auto_awesome,
                              size: 64,
                              color: AppTheme.aiColor.withValues(alpha: 0.5)),
                          const SizedBox(height: AppTheme.spacingMd),
                          Text(
                            '你好～我是宝宝的 AI 小助手',
                            style: Theme.of(context).textTheme.titleLarge,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: AppTheme.spacingSm),
                          Text(
                            '有什么育儿问题尽管问我吧！也可以试试下面的快捷提问~',
                            style: Theme.of(context).textTheme.bodySmall,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: AppTheme.spacingXl),
                          Wrap(
                            spacing: AppTheme.spacingSm,
                            runSpacing: AppTheme.spacingSm,
                            children:
                                quickPrompts.map((p) => _chip(p)).toList(),
                          ),
                        ],
                      ),
                    );
                  }

                  return ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.all(AppTheme.spacingLg),
                    itemCount: messages.length + (_isTyping ? 1 : 0),
                    itemBuilder: (context, index) {
                      if (index == messages.length && _isTyping) {
                        return _typingIndicator();
                      }
                      final msg = messages[index];
                      final isUser = msg['role'] == 'user';
                      return _messageBubble(msg['content'] ?? '', isUser);
                    },
                  );
                },
              ),
            ),

            // Input Bar
            Padding(
              padding: const EdgeInsets.all(AppTheme.spacingMd),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _controller,
                      decoration: InputDecoration(
                        hintText: '输入育儿问题...',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                        ),
                        contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16, vertical: 8),
                      ),
                      maxLines: null,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => _sendMessage(),
                    ),
                  ),
                  const SizedBox(width: AppTheme.spacingSm),
                  GestureDetector(
                    onTap: _sendMessage,
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: const BoxDecoration(
                        color: AppTheme.aiColor,
                        shape: BoxShape.circle,
                      ),
                      child:
                          const Icon(Icons.send, color: Colors.white, size: 20),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _messageBubble(String text, bool isUser) {
    return Row(
      mainAxisAlignment:
          isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
      children: [
        if (!isUser)
          Container(
            width: 32,
            height: 32,
            decoration: const BoxDecoration(
                color: AppTheme.aiColor, shape: BoxShape.circle),
            child:
                const Icon(Icons.auto_awesome, color: Colors.white, size: 16),
          ),
        const SizedBox(width: AppTheme.spacingSm),
        ConstrainedBox(
          constraints:
              BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.7),
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: isUser ? AppTheme.primary : Colors.white,
              borderRadius: BorderRadius.only(
                topLeft: const Radius.circular(12),
                topRight: const Radius.circular(12),
                bottomLeft: Radius.circular(isUser ? 12 : 4),
                bottomRight: Radius.circular(isUser ? 4 : 12),
              ),
            ),
            child: SelectableText(text,
                style: TextStyle(
                    color: isUser ? Colors.white : AppTheme.textPrimary)),
          ),
        ),
        const SizedBox(width: AppTheme.spacingSm),
        if (isUser)
          Container(
            width: 32,
            height: 32,
            decoration: const BoxDecoration(
                color: AppTheme.secondary, shape: BoxShape.circle),
            child: const Icon(Icons.person, color: Colors.white, size: 16),
          ),
      ],
    );
  }

  Widget _typingIndicator() {
    return const Row(
      children: [
        SizedBox(width: 40),
        _DotAnimation(),
        SizedBox(width: 4),
        _DotAnimation(delay: 0.2),
        SizedBox(width: 4),
        _DotAnimation(delay: 0.4),
      ],
    );
  }

  Widget _chip(String text) {
    return Material(
      color: AppTheme.aiColor.withValues(alpha: 0.1),
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: () {
          _controller.text = text;
          _sendMessage();
        },
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: Text(text, style: const TextStyle(fontSize: 13)),
        ),
      ),
    );
  }
}

class _DotAnimation extends StatefulWidget {
  final double delay;
  const _DotAnimation({this.delay = 0});

  @override
  State<_DotAnimation> createState() => _DotAnimationState();
}

class _DotAnimationState extends State<_DotAnimation>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
        vsync: this, duration: const Duration(milliseconds: 600));
    _animation = Tween<double>(begin: 0.3, end: 1.0).animate(
      CurvedAnimation(
          parent: _controller,
          curve: Interval(widget.delay, 1.0, curve: Curves.easeInOut)),
    );
    _controller.forward();
    _controller.addStatusListener((status) {
      if (status == AnimationStatus.completed) {
        _controller.reset();
        Future.delayed(Duration(milliseconds: (widget.delay * 1000).toInt()),
            () {
          if (mounted) _controller.forward();
        });
      }
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _animation,
      builder: (_, child) {
        return Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(
            color: AppTheme.aiColor.withValues(alpha: _animation.value),
            shape: BoxShape.circle,
          ),
        );
      },
    );
  }
}
