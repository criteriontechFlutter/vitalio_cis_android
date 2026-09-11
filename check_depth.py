with open('app/src/main/java/com/critetiontech/vitalio_cis/ui/screens/HomeView.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

depth = 0
for i, line in enumerate(lines):
    depth += line.count('{')
    depth -= line.count('}')
    
    if 'fun DashboardScreen' in line:
        print(f'DashboardScreen starts at {i+1}')
    elif 'fun HomeView' in line:
        print(f'HomeView starts at {i+1}, depth={depth}')
    elif 'fun VoiceAssistantSheet' in line:
        print(f'VoiceAssistantSheet starts at {i+1}, depth={depth}')
    elif 'fun BottomNavigationBar' in line:
        print(f'BottomNavigationBar starts at {i+1}, depth={depth}')
    
    if depth < 0:
        print(f"Warning: depth < 0 at line {i+1}")
