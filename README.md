# sk m&amp;service 하계 채용연계형 인턴십

## 제출자
| 제출자 이름 | 전화번호 |
|---|---|
|서호준|01064723783|

## task1
#### 내부 함수
* haversine 함수 : 지구 곡률을 반영한 거리 계산법으로 GPS 좌표 간 거리 측정합니다.
    > haversine 공식은 Wikipedia를 참고했습니다.
* projectToSegment 함수 : GPS 지점과 도로 선분 사이의 가장 근접한 투영점을 구하는 함수입니다.
    > 평면 좌표에 있다고 가정하여 계산한 거리를 haversine 함수를 통해 실제 거리로 치환합니다.
* matchGpsToRoad 함수 : GPS 지점을 모든 도로 선분에 비교해 가장 가까운 도로를 찾는 함수입니다.
    > 해당 도로가 따라야 할 경로에 포함되지 않는다면 이탈로 판단합니다.
    > 만약 thresholdMeters를 초과하는 경우는 노이즈나 건물 반사로 간주하여 이탈 처리합니다.

#### 흐름
1. OSM 데이터를 파싱합니다.
2. GPS 데이터를 파싱합니다.
3. 파싱한 데이터를 기반으로 위의 함수들을 활용해 매칭합니다.
4. 전체 데이터를 출력합니다.

## task2
#### 문제
입력이 발생할 때마다 페이지 일부를 다시 랜더링하면서 focus가 풀리고, 전반적으로 비효율적으로 동작하고 있는 것이 문제라고 생각했습니다.

그에 따라서 제거 후 생성하는 것이 아니라 업데이트하는 방법으로 변경했습니다.


#### 기능 추가 사항
* 검색이 완료된 컴포넌트를 선택할 시 상세 정보가 나오도록 변경했습니다.
```
li.addEventListener('click', () => showPersonDetail(person));

function showPersonDetail(person) {
  const detailBox = document.getElementById('personDetail');
  
  if (!person) {
    detailBox.innerHTML = `<div class="no-results">해당 인물에 대한 상세 정보가 없습니다.</div>`;
  } else {
    detailBox.innerHTML = `
      <div class="detail-card">
        <h2>${person.name}</h2>
        <p><strong>나이:</strong> ${person.age}</p>
        <p><strong>직업:</strong> ${person.gender == "W" ? "여성": "남성"}</p>
      </div>
    `;
  }

  detailBox.style.display = 'block';
}
```
  해당 코드를 사용하여 데이터가 있다면 정보를 보여줄 수 있도록 수정했습니다.


* csv 파일을 별도로 분리하여 관리했습니다.
```
async function loadCsvData(filePath){
    const res = await fetch(filePath);
    const text = await res.text();
    return parseCsv(text);
}

function parseCsv(csv){
  const lines = csv.trim().split('\n');
  const headers = lines[0].split(',').map(h => h.trim());
  
  const parsed = lines.slice(1).map(line => {
    const values = line.split(',').map(v => v.trim());
    return {
      name: values[0],
      age: values[1],
      gender: values[2],
    };
  });
  
  return parsed;
}
```
위의 함수를 통해 data.csv 파일을 가져와 파싱하고 정리했습니다.

data.csv 파일의 형식은

| 이름 | 나이 | 성별 |
|---|---|
| alice | 10 | W |

위의 예시와 같이 구성되어 있습니다.
