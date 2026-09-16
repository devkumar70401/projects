export default function ColorLegend() {
  return (
    <div className="flex flex-wrap items-center gap-3 text-xs bg-slate-50 border border-slate-200 px-3 py-1.5 rounded-lg text-slate-700">
      <span className="font-semibold text-slate-900 font-devanagari">रंग संकेत (Color Code):</span>
      <div className="flex items-center space-x-1">
        <span className="w-2.5 h-2.5 rounded-full bg-red-600 inline-block"></span>
        <span>पिछला शेष / शेष (लाल / Red: सिस्टम बैलेंस)</span>
      </div>
      <div className="flex items-center space-x-1">
        <span className="w-2.5 h-2.5 rounded-full bg-blue-600 inline-block"></span>
        <span>आमद / बिक्री (नीला / Blue: प्रविष्टि इनपुट)</span>
      </div>
      <div className="flex items-center space-x-1">
        <span className="w-2.5 h-2.5 rounded-full bg-slate-700 inline-block"></span>
        <span>योग / दर / कीमत (काला: गणना व दर)</span>
      </div>
      <div className="flex items-center space-x-1 ml-auto text-slate-500 font-mono">
        <span>[Tab / Enter = अगली सेल]</span>
      </div>
    </div>
  );
}
