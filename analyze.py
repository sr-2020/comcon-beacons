max_diff = 0

with open('data.json') as f:
    data = json.load(f)

total = 0

print(data[112]['created_at'][11:])

for i in range(len(data) - 1):
    if not data[i]['created_at'].startswith('2019-02-06'):
        break
    t = time.strptime(data[i]['created_at'][11:], '%H:%M:%S')
    t2 = time.strptime(data[i + 1]['created_at'][11:], '%H:%M:%S')
    d = calendar.timegm(t) - calendar.timegm(t2)
    if (d > max_diff):
        print('--> ', i, ' ', max_diff, ' ', data[i]['created_at'])
        max_diff = d
    total += d

print(max_diff)

