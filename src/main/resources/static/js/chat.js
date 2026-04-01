// ==================== 页面切换功能 ====================
function switchPage(pageName) {
    // 更新导航项激活状态
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.page === pageName) {
            item.classList.add('active');
        }
    });

    // 切换页面显示
    document.querySelectorAll('.page').forEach(page => {
        page.classList.remove('active');
    });
    document.getElementById(pageName + 'Page').classList.add('active');

    Debug.info('切换到页面', { page: pageName });
}

// ==================== 调试工具 ====================
const Debug = {
    log: function(type, message, data) {
        const logEl = document.getElementById('debugLog');
        const timestamp = new Date().toLocaleTimeString('zh-CN', {
            hour12: false,
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            fractionalSecondDigits: 3
        });

        let content = `<span class="log-timestamp">[${timestamp}]</span>`;
        content += `<strong>[${type.toUpperCase()}]</strong> ${escapeHtml(message)}`;

        if (data !== undefined) {
            content += `<br><span style="color: #6b7280;">${escapeHtml(JSON.stringify(data, null, 2))}</span>`;
        }

        const entry = document.createElement('div');
        entry.className = `log-entry ${type}`;
        entry.innerHTML = content;
        logEl.appendChild(entry);
        logEl.scrollTop = logEl.scrollHeight;

        console.log(`[${type}] ${message}`, data || '');
    },
    info: function(msg, data) { this.log('info', msg, data); },
    success: function(msg, data) { this.log('success', msg, data); },
    warn: function(msg, data) { this.log('warn', msg, data); },
    error: function(msg, data) { this.log('error', msg, data); },
    data: function(msg, data) { this.log('data', msg, data); }
};

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ==================== Markdown 渲染 ====================
let citationCounter = 0;
const citationMap = new Map(); // 存储引用编号和 URL 的映射

function renderMarkdown(text) {
    if (typeof marked !== 'undefined') {
        // 配置 marked 选项
        marked.setOptions({
            gfm: true,
            breaks: true,
            headerIds: false,
            mangle: false
        });
        
        // 先使用 marked 解析 Markdown
        let html = marked.parse(text);
        
        console.log('marked 解析后的 HTML:', html);
        
        // 处理参考文献列表 [^1]: xxx 或 [1] xxx -> 转换为带 ID 的列表项
        // 处理 <br>[1] 或 >[1] 格式的参考文献（在段落内或引用块内）
        html = html.replace(/&lt;br&gt;\[([0-9]+)\]\s*<a href=/g, function(match, number) {
            console.log('匹配到 <br>[1] 格式:', match);
            return '<br><span class="reference-item" id="ref-' + number + '"><strong>[' + number + ']</strong> <a href=';
        });
        
        // 处理 [^1]: xxx 格式
        html = html.replace(/^\[\^([0-9]+)\]:\s*(.+)$/gm, function(match, number, content) {
            console.log('匹配到 [^' + number + '] 格式:', match);
            return '<p class="reference-item" id="ref-' + number + '"><strong>[^' + number + ']</strong> ' + content.trim() + '</p>';
        });
        
        // 处理行首 [1] xxx 格式（不在 <a> 标签内）
        html = html.replace(/^(\s*)\[([0-9]+)\]\s*(.+)$/gm, function(match, indent, number, content) {
            // 检查是否是链接语法的一部分
            if (content.startsWith('(')) {
                return match;
            }
            console.log('匹配到行首 [' + number + '] 格式:', match);
            return '<p class="reference-item" id="ref-' + number + '"><strong>[' + number + ']</strong> ' + content.trim() + '</p>';
        });
        
        // 处理 Markdown 脚注引用 [^1], [^2] 等 -> 转换为可点击的上标链接
        html = html.replace(/\[\^([0-9]+)\]/g, function(match, number) {
            const id = 'citation-' + (++citationCounter);
            const targetId = 'ref-' + number;
            citationMap.set(number, { id: id, targetId: targetId });
            return '<sup class="citation" id="' + id + '"><a href="#' + targetId + '">[^' + number + ']</a></sup>';
        });
        
        // 处理普通引用 [1], [2] 等（排除链接语法）
        html = html.replace(/\[([0-9]+)\]/g, function(match, number, offset, string) {
            // 检查后面是否紧跟 ( ，如果是则是链接语法，不处理
            const afterMatch = string.substring(offset + match.length);
            if (afterMatch.startsWith('(')) {
                return match; // 这是链接，保持原样
            }
            // 检查是否在 <a> 标签内（已经被处理过的）
            const beforeMatch = string.substring(0, offset);
            if (beforeMatch.endsWith('">') || beforeMatch.endsWith('>')) {
                return match; // 在标签内，保持原样
            }
            // 检查是否已经在 div 标签内（参考文献列表）
            if (beforeMatch.endsWith('<div class="reference-item" id="ref-') || 
                beforeMatch.endsWith('<strong>')) {
                return match;
            }
            // 检查是否已经在上标标签内
            if (beforeMatch.endsWith('<sup class="citation">')) {
                return match;
            }
            const id = 'citation-' + (++citationCounter);
            const targetId = 'ref-' + number;
            citationMap.set(number, { id: id, targetId: targetId });
            return '<sup class="citation" id="' + id + '"><a href="#' + targetId + '">[' + number + ']</a></sup>';
        });
        
        console.log('最终渲染的 HTML:', html);
        return html;
    }
    // 降级处理：纯文本
    return escapeHtml(text).replace(/\n/g, '<br>');
}

// 滚动到参考文献位置
window.scrollToReference = function(event, targetId) {
    event.preventDefault();
    console.log('尝试跳转到 ID:', targetId);
    const targetEl = document.getElementById(targetId);
    console.log('找到元素:', targetEl);
    if (targetEl) {
        targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
        // 高亮显示
        targetEl.style.transition = 'background-color 0.5s';
        targetEl.style.backgroundColor = '#3b82f6';
        setTimeout(() => {
            targetEl.style.backgroundColor = 'transparent';
        }, 2000);
    } else {
        console.warn('未找到参考文献元素:', targetId);
        Debug.warn('未找到参考文献 #' + targetId);
    }
}

// 使用事件委托处理引用点击
function initCitationClick() {
    document.addEventListener('click', function(event) {
        const citationLink = event.target.closest('.citation a');
        if (citationLink) {
            event.preventDefault();
            const targetId = citationLink.getAttribute('href').substring(1);
            console.log('点击引用，跳转到 ID:', targetId);
            window.scrollToReference(event, targetId);
        }
    });
}

// ==================== 聊天功能 ====================
let isSending = false;
let renderTimer = null;
let pendingContent = '';
let currentMsgDiv = null;

// DOM 元素
let chatBox, userInput, sendBtn, debugToggle, debugLog;

// 初始化 DOM 引用
function initDOM() {
    chatBox = document.getElementById('chatBox');
    userInput = document.getElementById('userInput');
    sendBtn = document.getElementById('sendBtn');
    debugToggle = document.getElementById('debugToggle');
    debugLog = document.getElementById('debugLog');
}

// 切换调试面板
function toggleDebugPanel() {
    const isHidden = debugLog.classList.toggle('hidden');
    debugToggle.textContent = isHidden ? '▶' : '▼';
}

// 添加消息到聊天框
function addMessage(role, content, isMarkdown = true) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${role}`;
    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content' + (isMarkdown ? ' markdown-body' : '');

    if (isMarkdown) {
        contentDiv.innerHTML = renderMarkdown(content);
    } else {
        contentDiv.textContent = content;
    }

    msgDiv.appendChild(contentDiv);
    chatBox.appendChild(msgDiv);
    chatBox.scrollTop = chatBox.scrollHeight;
    return msgDiv;
}

// 更新助手消息内容（用于流式更新）
function updateAssistantMessage(content) {
    if (!currentMsgDiv) return;
    const contentDiv = currentMsgDiv.querySelector('.message-content');
    if (contentDiv) {
        // 确保有 markdown-body 类
        if (!contentDiv.classList.contains('markdown-body')) {
            contentDiv.classList.add('markdown-body');
        }
        const html = renderMarkdown(content);
        contentDiv.innerHTML = html;
        chatBox.scrollTop = chatBox.scrollHeight;
    }
}

// 防抖渲染：避免频繁解析 Markdown
function scheduleRender(content) {
    pendingContent = content;
    if (renderTimer) return;

    renderTimer = setTimeout(function() {
        updateAssistantMessage(pendingContent);
        renderTimer = null;
    }, 50); // 50ms 防抖
}

// 发送聊天请求
function sendChat() {
    const question = userInput.value.trim();
    if (!question || isSending) return;

    // 重置引用计数器
    citationCounter = 0;
    citationMap.clear();

    // 添加用户消息
    addMessage('user', question, false);  // 用户消息不需要 Markdown
    userInput.value = '';

    // 创建助手消息占位
    currentMsgDiv = addMessage('assistant', '思考中...', true);  // 助手消息使用 Markdown

    // 禁用输入
    isSending = true;
    sendBtn.disabled = true;
    userInput.disabled = true;

    Debug.info('开始发送请求', { question: question });

    const url = '/api/chat/stream';
    Debug.info('连接 SSE 端点', { url: url });

    fetchWithStream(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: generateSessionId(), question: question })
    });
}

function generateSessionId() {
    return 'sess_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

async function fetchWithStream(url, options) {
    let accumulatedContent = '';
    let chunkCount = 0;
    let firstChunkTime = null;
    let lastChunkTime = null;
    let buffer = '';

    try {
        Debug.info('发起 fetch 请求');
        const response = await fetch(url, options);

        Debug.info('收到响应', {
            status: response.status,
            statusText: response.statusText,
            contentType: response.headers.get('content-type')
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }

        if (!response.body) {
            throw new Error('浏览器不支持 ReadableStream');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');

        Debug.success('成功获取响应流，开始读取数据...');

        while (true) {
            Debug.info('等待读取下一个数据块...');
            const { done, value } = await reader.read();

            if (done) {
                Debug.success('流读取完成');
                break;
            }

            const chunkTime = Date.now();
            if (!firstChunkTime) firstChunkTime = chunkTime;
            lastChunkTime = chunkTime;
            chunkCount++;

            const chunk = decoder.decode(value, { stream: true });
            Debug.data(`收到数据块 #${chunkCount}`, {
                raw: chunk,
                byteLength: value.length
            });

            buffer += chunk;

            // 处理 SSE 格式
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            for (const line of lines) {
                const trimmedLine = line.trim();
                if (!trimmedLine) continue;

                if (trimmedLine.startsWith('data:')) {
                    const dataStr = trimmedLine.slice(5).trim();
                    Debug.info('解析 SSE data 行', { dataStr: dataStr });

                    try {
                        const payload = JSON.parse(dataStr);
                        Debug.data('解析 JSON payload', payload);

                        if (payload.type === 'content' && payload.data) {
                            accumulatedContent += payload.data;
                            scheduleRender(accumulatedContent);
                            Debug.success('累积内容', {
                                length: accumulatedContent.length
                            });
                        } else if (payload.type === 'error') {
                            Debug.error('收到错误消息', payload);
                            updateAssistantMessage('❌ 错误：' + payload.data);
                        } else if (payload.type === 'done') {
                            Debug.success('收到完成信号');
                        }
                    } catch (e) {
                        Debug.warn('JSON 解析失败', { error: e.message, data: dataStr });
                        accumulatedContent += dataStr;
                        scheduleRender(accumulatedContent);
                    }
                }
            }
        }

        // 处理剩余缓冲
        if (buffer.trim() && buffer.startsWith('data:')) {
            const dataStr = buffer.slice(5).trim();
            try {
                const payload = JSON.parse(dataStr);
                if (payload.type === 'content' && payload.data) {
                    accumulatedContent += payload.data;
                }
            } catch (e) {
                accumulatedContent += dataStr;
            }
            scheduleRender(accumulatedContent);
        }

        // 确保最后一次渲染完成
        if (renderTimer) {
            clearTimeout(renderTimer);
            updateAssistantMessage(accumulatedContent);
            renderTimer = null;
        }

        Debug.success('请求处理完成', {
            totalChunks: chunkCount,
            totalContent: accumulatedContent.length,
            duration: lastChunkTime && firstChunkTime ? (lastChunkTime - firstChunkTime) + 'ms' : 'N/A'
        });

        if (accumulatedContent.length === 0) {
            updateAssistantMessage('未收到任何回复内容');
        }

    } catch (error) {
        Debug.error('请求失败', { message: error.message });
        updateAssistantMessage('❌ 请求失败：' + error.message);
    } finally {
        isSending = false;
        sendBtn.disabled = false;
        userInput.disabled = false;
        userInput.focus();
        pendingContent = '';
        currentMsgDiv = null;
    }
}

// ==================== 页面初始化 ====================
document.addEventListener('DOMContentLoaded', function() {
    initDOM();
    initCitationClick();  // 初始化引用点击事件
    initSidebar();        // 初始化侧边栏
    initSopForm();        // 初始化 SOP 表单
    initSopSearchForm();  // 初始化 SOP 搜索表单
    initUploadForm();     // 初始化文件上传表单

    Debug.success('页面加载完成，准备就绪');
});

// ==================== 侧边栏功能 ====================
function initSidebar() {
    // 绑定导航项点击事件
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();
            const pageName = this.dataset.page;
            switchPage(pageName);
        });
    });
}

// ==================== SOP 提交功能 ====================
function initSopForm() {
    const sopForm = document.getElementById('sopForm');
    const sopSubmitBtn = document.getElementById('sopSubmitBtn');
    const sopResult = document.getElementById('sopResult');
    const sopUserName = document.getElementById('sopUserName');
    const sopContent = document.getElementById('sopContent');

    sopForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        const userName = sopUserName.value.trim() || 'system';
        const content = sopContent.value.trim();

        if (!content) {
            Debug.error('SOP 内容不能为空');
            showSopResult('error', 'SOP 内容不能为空');
            return;
        }

        // 禁用提交按钮
        sopSubmitBtn.disabled = true;
        sopSubmitBtn.textContent = '提交中...';

        Debug.info('提交 SOP', { userName: userName, contentLength: content.length });

        try {
            const response = await fetch('/api/sop/submit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userName: userName,
                    content: content
                })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }

            const result = await response.json();
            Debug.success('SOP 提交成功', result);

            if (result.success) {
                showSopResult('success', `SOP 提交成功！ID: ${result.addId}`);
                // 清空内容区域
                sopContent.value = '';
            } else {
                showSopResult('error', result.errorMessage || '提交失败，请重试');
            }
        } catch (error) {
            Debug.error('SOP 提交失败', { message: error.message });
            showSopResult('error', '提交失败：' + error.message);
        } finally {
            sopSubmitBtn.disabled = false;
            sopSubmitBtn.textContent = '提交 SOP';
        }
    });
}

function showSopResult(type, message) {
    const sopResult = document.getElementById('sopResult');
    sopResult.className = 'sop-result ' + type;
    sopResult.textContent = message;
}

// ==================== SOP 搜索功能 ====================
function initSopSearchForm() {
    const sopSearchForm = document.getElementById('sopSearchForm');
    const sopSearchBtn = document.getElementById('sopSearchBtn');
    const searchResult = document.getElementById('searchResult');
    const searchContent = document.getElementById('searchContent');

    sopSearchForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        const content = searchContent.value.trim();

        if (!content) {
            Debug.error('搜索内容不能为空');
            showSearchResult('error', '搜索内容不能为空');
            return;
        }

        // 禁用提交按钮
        sopSearchBtn.disabled = true;
        sopSearchBtn.textContent = '搜索中...';

        Debug.info('搜索 SOP', { contentLength: content.length });

        try {
            const response = await fetch('/api/sop/search', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    searchContent: content
                })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }

            const result = await response.json();
            Debug.success('SOP 搜索成功', result);

            displaySearchResults(result.searchResponseList);
        } catch (error) {
            Debug.error('SOP 搜索失败', { message: error.message });
            showSearchResult('error', '搜索失败：' + error.message);
        } finally {
            sopSearchBtn.disabled = false;
            sopSearchBtn.textContent = '搜索';
        }
    });
}

function displaySearchResults(results) {
    const searchResult = document.getElementById('searchResult');
    
    if (!results || results.length === 0) {
        searchResult.className = 'search-result has-results';
        searchResult.innerHTML = '<div class="search-no-result">未找到相似的 SOP</div>';
        return;
    }

    searchResult.className = 'search-result has-results';
    
    let html = '';
    results.forEach((item, index) => {
        html += `
            <div class="search-result-item">
                <div class="search-result-header">
                    <span class="search-result-user">👤 ${escapeHtml(item.userName || '未知用户')}</span>
                    <span class="search-result-index">结果 #${index + 1}</span>
                </div>
                <div class="search-result-content">${escapeHtml(item.content)}</div>
            </div>
        `;
    });

    searchResult.innerHTML = html;
}

function showSearchResult(type, message) {
    const searchResult = document.getElementById('searchResult');
    searchResult.className = 'search-result has-results';
    
    if (type === 'error') {
        searchResult.innerHTML = `<div class="sop-result error">${message}</div>`;
    } else {
        searchResult.innerHTML = `<div class="sop-result success">${message}</div>`;
    }
}

// ==================== 文件上传功能 ====================
function initUploadForm() {
    const uploadForm = document.getElementById('uploadForm');
    const uploadFile = document.getElementById('uploadFile');
    const uploadBtn = document.getElementById('uploadBtn');
    const fileDropZone = document.getElementById('fileDropZone');
    const uploadProgressContainer = document.getElementById('uploadProgressContainer');
    const progressFill = document.getElementById('progressFill');
    const progressPercent = document.getElementById('progressPercent');
    const progressStatus = document.getElementById('progressStatus');
    const progressDetail = document.getElementById('progressDetail');
    const uploadResult = document.getElementById('uploadResult');

    let selectedFile = null;

    // 点击文件选择区域
    fileDropZone.addEventListener('click', function() {
        uploadFile.click();
    });

    // 文件选择变化
    uploadFile.addEventListener('change', function(e) {
        if (e.target.files && e.target.files.length > 0) {
            selectedFile = e.target.files[0];
            handleFileSelect(selectedFile);
        }
    });

    // 拖拽事件
    fileDropZone.addEventListener('dragover', function(e) {
        e.preventDefault();
        e.stopPropagation();
        this.classList.add('drag-over');
    });

    fileDropZone.addEventListener('dragleave', function(e) {
        e.preventDefault();
        e.stopPropagation();
        this.classList.remove('drag-over');
    });

    fileDropZone.addEventListener('drop', function(e) {
        e.preventDefault();
        e.stopPropagation();
        this.classList.remove('drag-over');
        
        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
            selectedFile = e.dataTransfer.files[0];
            uploadFile.files = e.dataTransfer.files;
            handleFileSelect(selectedFile);
        }
    });

    // 处理文件选择
    function handleFileSelect(file) {
        const fileSize = formatFileSize(file.size);
        const fileName = file.name;
        
        // 显示文件名
        const existingDisplay = fileDropZone.querySelector('.file-name-display');
        if (existingDisplay) {
            existingDisplay.remove();
        }
        
        const nameDisplay = document.createElement('div');
        nameDisplay.className = 'file-name-display';
        nameDisplay.innerHTML = `📎 ${fileName} (${fileSize})`;
        fileDropZone.appendChild(nameDisplay);
        
        Debug.info('文件已选择', { name: fileName, size: file.size });
    }

    // 表单提交
    uploadForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        if (!selectedFile) {
            showUploadResult('error', '请先选择文件');
            return;
        }

        // 显示进度条
        uploadProgressContainer.style.display = 'block';
        uploadResult.classList.remove('has-content');
        uploadBtn.disabled = true;
        uploadBtn.textContent = '上传中...';

        // 重置进度条
        updateProgress(0, '准备上传...', '');

        Debug.info('开始上传文件', { name: selectedFile.name, size: selectedFile.size });

        const formData = new FormData();
        formData.append('file', selectedFile);

        // 使用 XMLHttpRequest 来实现带进度条的上传
        const xhr = new XMLHttpRequest();
        
        // 设置超时时间为 5 分钟（300 秒）
        xhr.timeout = 300000;

        // 上传进度监听
        xhr.upload.addEventListener('progress', function(e) {
            if (e.lengthComputable) {
                const percentComplete = (e.loaded / e.total) * 100;
                const loadedSize = formatFileSize(e.loaded);
                const totalSize = formatFileSize(e.total);
                updateProgress(percentComplete, '上传中...', `${loadedSize} / ${totalSize}`);
            }
        });

        // 上传完成
        xhr.addEventListener('load', function() {
            Debug.info('上传请求完成', { status: xhr.status });
            
            if (xhr.status >= 200 && xhr.status < 300) {
                try {
                    const response = JSON.parse(xhr.responseText);
                    Debug.success('文件上传成功', response);
                    
                    if (response.code === 200 && response.data) {
                        const data = response.data;
                        showUploadResult('success', `
                            <div class="upload-result-info">
                                <div class="upload-result-item">
                                    <span class="upload-result-label">文件名</span>
                                    <span class="upload-result-value">${escapeHtml(data.fileName)}</span>
                                </div>
                                <div class="upload-result-item">
                                    <span class="upload-result-label">文件大小</span>
                                    <span class="upload-result-value">${formatFileSize(data.fileSize)}</span>
                                </div>
                                <div class="upload-result-item">
                                    <span class="upload-result-label">存储路径</span>
                                    <span class="upload-result-value">${escapeHtml(data.filePath)}</span>
                                </div>
                            </div>
                        `);
                        updateProgress(100, '上传完成！', '');
                        
                        // 清空文件选择
                        selectedFile = null;
                        uploadFile.value = '';
                        const existingDisplay = fileDropZone.querySelector('.file-name-display');
                        if (existingDisplay) {
                            existingDisplay.remove();
                        }
                    } else {
                        showUploadResult('error', response.message || '上传失败');
                    }
                } catch (e) {
                    Debug.error('响应解析失败', { error: e.message });
                    showUploadResult('error', '响应解析失败：' + e.message);
                }
            } else {
                showUploadResult('error', '上传失败：HTTP ' + xhr.status);
            }
            
            uploadBtn.disabled = false;
            uploadBtn.textContent = '上传并处理';
        });

        // 上传错误
        xhr.addEventListener('error', function() {
            Debug.error('上传请求错误');
            showUploadResult('error', '网络错误，请检查网络连接');
            uploadBtn.disabled = false;
            uploadBtn.textContent = '上传并处理';
        });

        // 上传超时
        xhr.addEventListener('timeout', function() {
            Debug.error('上传请求超时');
            showUploadResult('error', '请求超时，文件可能较大，请重试');
            uploadBtn.disabled = false;
            uploadBtn.textContent = '上传并处理';
        });

        // 发送请求
        xhr.open('POST', '/api/sop/upload');
        xhr.send(formData);
    });

    // 更新进度条
    function updateProgress(percent, status, detail) {
        progressFill.style.width = percent + '%';
        progressPercent.textContent = Math.round(percent) + '%';
        progressStatus.textContent = status;
        progressDetail.textContent = detail;
    }
}

// 格式化文件大小
function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}

// 显示上传结果
function showUploadResult(type, message) {
    const uploadResult = document.getElementById('uploadResult');
    uploadResult.className = 'upload-result has-content ' + type;
    uploadResult.innerHTML = message;
}
