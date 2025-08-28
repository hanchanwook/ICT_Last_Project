import { LexicalComposer } from "@lexical/react/LexicalComposer";
import { RichTextPlugin } from "@lexical/react/LexicalRichTextPlugin";
import { HistoryPlugin } from "@lexical/react/LexicalHistoryPlugin";
import { OnChangePlugin } from "@lexical/react/LexicalOnChangePlugin";
import { ContentEditable } from "@lexical/react/LexicalContentEditable";
import { $generateHtmlFromNodes } from "@lexical/html"; // 추가

function Placeholder() {
  return <div className="absolute top-2 left-2 text-gray-400">팝업 내용을 입력하세요...</div>;
}

export default function TextEditor({ value, onChange }) {
  const initialConfig = {
    namespace: "PopupEditor",
    theme: {
      paragraph: "mb-2"
    },
    onError: (error) => console.error(error)
  };

  return (
    <LexicalComposer initialConfig={initialConfig}>
      <div className="relative border rounded p-2 min-h-[150px]">
        <RichTextPlugin
          contentEditable={<ContentEditable className="outline-none min-h-[120px]" />}
          placeholder={<Placeholder />}
        />
        <HistoryPlugin />
        <OnChangePlugin
          onChange={(editorState, editor) => {
            editorState.read(() => {
              // === JSON 대신 HTML 생성 ===
              const html = $generateHtmlFromNodes(editor);
              onChange(html); // HTML을 부모로 전달 (DB에 HTML 저장)
            });
          }}
        />
      </div>
    </LexicalComposer>
  );
}
