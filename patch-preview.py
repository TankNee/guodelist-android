from pathlib import Path

p = Path('app/src/main/res/layout/widget_today_small.xml')
s = p.read_text(encoding='utf-8')
s = s.replace('android:id="@+id/widget_count"', 'android:id="@+id/widget_count"\n            android:text="4"')
s = s.replace('android:id="@+id/widget_count_label"', 'android:id="@+id/widget_count_label"\n            android:text="@string/widget_open_count"')
s = s.replace('android:max="100"', 'android:max="100"\n        android:progress="45"')
s = s.replace('android:id="@+id/widget_next"', 'android:id="@+id/widget_next"\n        android:text="@string/widget_preview_task_1"')
p.write_text(s, encoding='utf-8')

p = Path('app/src/main/res/layout/widget_today_large.xml')
s = p.read_text(encoding='utf-8')
s = s.replace('android:id="@+id/widget_count"', 'android:id="@+id/widget_count"\n            android:text="4"')
s = s.replace('android:id="@+id/widget_count_label"', 'android:id="@+id/widget_count_label"\n            android:text="@string/widget_open_count"')
s = s.replace('android:id="@+id/widget_date"', 'android:id="@+id/widget_date"\n            android:text="@string/widget_preview_date"')
s = s.replace('android:max="100"', 'android:max="100"\n        android:progress="45"')
p.write_text(s, encoding='utf-8')

p = Path('app/src/main/res/layout/widget_quick_add.xml')
s = p.read_text(encoding='utf-8')
s = s.replace('android:id="@+id/widget_quick_sub"', 'android:id="@+id/widget_quick_sub"\n        android:text="今天 4 项待办"')
p.write_text(s, encoding='utf-8')
print('patched')
