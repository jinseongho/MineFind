package me.ingeni.minefind;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by jinseongho on 2016. 5. 10..
 */

public class MainActivity extends Activity {

    private int MAP_WIDTH = 11; // 게임 맵 가로 크기
    private int MAP_HEIGHT = 12; // 게임 맵 세로 크기
    private int MINE_MAP_RANGE = MAP_WIDTH * MAP_HEIGHT; // 게임 맵 크기
    private int MINE_COUNT = 13; // 지뢰 갯수
    private int MINE_CONST = MAP_WIDTH + 1; // 지뢰찾기게임의 규칙에 의한 지뢰 상수


    private List<Integer> mineList; // 0~99까지 섞인 숫자를 넣은 List
    private List<Integer> mineFlagList = new ArrayList<>(); // 깃발을 세운 지점이 담긴 List
    private List<Integer> mapClickList = new ArrayList<>(); // 클릭한 위치를 담은 List
    private String[] mineArray; // 전체 게임 맵 크기만큼 지뢰가 있는지 없는지 정보를 담은 array
    private int actionCount = 0; // 지뢰를 안 밟고 끝까지 게임을 가는 경우 게임 횟수를 카운트하여 끝난 지점을 알기 위한 변수
    private MineGridView mineMapGrid;
    private MineMapAdapter mineMapAdapter;

    private TextView timerTxt;
    private String strTime;
    private Vibrator vibrator;
    private Timer mTimer;
    private TimerTask mTimeTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mineMapGrid = (MineGridView) findViewById(R.id.mineMapGrid);
        timerTxt = (TextView) findViewById(R.id.timerTxt);

        mineInit(MINE_MAP_RANGE, MINE_COUNT);

        /**
         * ItemClick을 하면 mapClickList와 mineFlagList에 현재 위치값이 없으면 mineMapClick()을 실행합니다.
         * **/

        mineMapGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (!mapClickList.contains(position) && !mineFlagList.contains(position)) {
                    mineMapClick(position, parent);
                }
            }
        });

        /**
         * LongItemClick을 하면 깃발을 세우거나 깃발을 취소할 수 있도록 만들었습니다.
         * **/

        mineMapGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
                final ImageView mineItem = (ImageView) parent.getChildAt(position).findViewById(R.id.mineBg);
                TextView mineTxt = (TextView) parent.getChildAt(position).findViewById(R.id.mineTxt);

                if (mineTxt.getText().length() == 0) {
                    if (!mineFlagList.contains(position)) {
                        mineItem.setBackgroundResource(R.drawable.flag);
                        vibrator.vibrate(100);
                        actionCount++;
                        mineFlagList.add(position);
                        if (actionCount == MINE_MAP_RANGE) {
                            gameOver(parent);
                        }
                    } else {
                        mineItem.setBackgroundColor(Color.parseColor("#000000"));
                        vibrator.vibrate(100);
                        actionCount--;
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                mineFlagList.remove((Object) position);
                            }
                        }, 500);

                    }
                }
                return false;
            }
        });
    }


    public void mineMapClick(final int position, final AdapterView<?> parent) {
        /**
         * 처음 터치하였을 때 지뢰가 나오면 다시 시작하여 값을 받습니다. (좀 더 효율적인 방법이 있겠지만 ..)
         * 빈 값 부분을 눌렀는데 깃발을 세운 지점일 경우 게임에 영향을 미치는 중복(actionCount, mineFindList에 담겨진 깃발 세운 position)을 제거합니다.
         * **/

        actionCount++;
        if (actionCount == 1 && mineArray[position].equals("mine")) {
            reTry(null);
        } else {
            int mineCountResult = 0;
            if (!mapClickList.contains(position)) {
                mapClickList.add(position);
            }

            if (actionCount == MINE_MAP_RANGE) {
                gameOver(parent);
            }
            if (mineFlagList.contains(position)) {
                actionCount--;
                mineFlagList.remove((Object) position);
            }

            mineCountResult = mineCountSearch(position, MINE_MAP_RANGE);
            ImageView mineItem = (ImageView) parent.getChildAt(position).findViewById(R.id.mineBg);
            TextView mineTxt = (TextView) parent.getChildAt(position).findViewById(R.id.mineTxt);

            if (mineArray[position].equals("mine")) {
                gameOver(parent);
            } else {
                if (mineCountResult == 0) {
                    mineTxt.setText("" + mineCountResult);
                    mineTxt.setTextColor(Color.parseColor("#4D5052"));
                    mineItem.setBackgroundColor(Color.parseColor("#4D5052"));
                    mineEmptySearch(position, MINE_MAP_RANGE, parent);
                } else {
                    mineTxt.setText("" + mineCountResult);
                    mineItem.setBackgroundColor(Color.parseColor("#000000"));
                }
            }
        }
    }

    public void mineInit(final int mineMapRange, final int mineCount) {

        /**
         * 0~99까지의 숫자를 랜덤하게 섞은 후 mineList에 담습니다.
         * mineList의 0~9번째 섞여진 숫자를 가져와 mineArray의 인덱스로 설정하고 "mine"으로 초기화 시켜줍니다.
         * gridView를 adapter에 연결하여 게임 맵 사이즈 만큼 화면에 나타냅니다.
         * **/

        mineArray = new String[mineMapRange];
        ArrayList<Integer> randomNumber = new ArrayList<>();

        for (int i = 0; i < mineMapRange; i++) {
            mineArray[i] = "";
            randomNumber.add(i);
        }

        Collections.shuffle(randomNumber);
        mineList = randomNumber.subList(0, mineCount);
        Collections.sort(mineList);

        for (int k = 0; k < mineCount; k++) {
            mineArray[mineList.get(k)] = "mine";
        }

        mineMapAdapter = new MineMapAdapter(MainActivity.this, MINE_MAP_RANGE);
        mineMapGrid.setAdapter(mineMapAdapter);

        mTimeTask = new TimerTask() {
            int time = 0;

            public void run() {
                try {
                    time++;
                    int min = time / 60;
                    int sec = time % 60;
                    strTime = String.format("%sm %ss", min, sec);
                    runOnUiThread(new TimerTask() {
                        @Override
                        public void run() {
                            timerTxt.setText(strTime);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        mTimer = new Timer();
        mTimer.schedule(mTimeTask, 0, 1000);
    }


    public int mineCountSearch(int position, int mineMapRange) {

        /**
         * 터치한 자신을 뺀 둘러쌓인 8방향에 대해서 값을 구합니다.
         * 10 x 10의 맵에서는 왼쪽 위 대각선(오른쪽 아래 대각선) 위치와 터치한 부분의 위치 값의 차이는 11만큼 납니다.
         * 이러한 차이 값은 게임맵 넓이 + 1 만큼 증가합니다. 이러한 값을
         * MINE_CONST라고 정하고 8방향에 대해 mineArray안에 "mine"이 있는지 확인합니다.
         * 또한 왼쪽벽과 오른쪽벽 넘어서 지뢰 갯수를 세지 않도록 예외처리를 해줍니다.
         * **/

        int mineSearchCount = 0;
        if (((position + 1) % (MINE_CONST - 1)) == 0) {

            if (position - MINE_CONST > -1 && mineArray[position - MINE_CONST].equals("mine")) {
                mineSearchCount++;
            }
            if (position - MINE_CONST + 1 > -1 && mineArray[position - MINE_CONST + 1].equals("mine")) {
                mineSearchCount++;
            }
            if (position - 1 > -1 && mineArray[position - 1].equals("mine")) {
                mineSearchCount++;
            }
            if (position + MINE_CONST - 1 < mineMapRange && mineArray[position + MINE_CONST - 1].equals("mine")) {
                mineSearchCount++;
            }
            if (position + MINE_CONST - 2 < mineMapRange && mineArray[position + MINE_CONST - 2].equals("mine")) {
                mineSearchCount++;
            }
        } else if (((position + 1) % (MINE_CONST - 1)) == 1) {

            if (position - MINE_CONST + 1 > -1 && mineArray[position - MINE_CONST + 1].equals("mine")) {
                mineSearchCount++;
            }
            if (position - MINE_CONST + 2 > -1 && mineArray[position - MINE_CONST + 2].equals("mine")) {
                mineSearchCount++;
            }
            if (position + 1 < mineMapRange && mineArray[position + 1].equals("mine")) {
                mineSearchCount++;
            }
            if (position + MINE_CONST < mineMapRange && mineArray[position + MINE_CONST].equals("mine")) {
                mineSearchCount++;
            }
            if (position + MINE_CONST - 1 < mineMapRange && mineArray[position + MINE_CONST - 1].equals("mine")) {
                mineSearchCount++;
            }
        } else {

            if (position - MINE_CONST > -1 && mineArray[position - MINE_CONST].equals("mine")) {
                mineSearchCount++;
            }
            if (position - MINE_CONST + 1 > -1 && mineArray[position - MINE_CONST + 1].equals("mine")) {
                mineSearchCount++;
            }
            if (position - MINE_CONST + 2 > -1 && mineArray[position - MINE_CONST + 2].equals("mine")) {
                mineSearchCount++;
            }
            if (position - 1 > -1 && mineArray[position - 1].equals("mine")) {
                mineSearchCount++;
            }
            if (position + 1 < mineMapRange && mineArray[position + 1].equals("mine")) {
                mineSearchCount++;
            }
            if (position + MINE_CONST < mineMapRange && mineArray[position + MINE_CONST].equals("mine")) {
                mineSearchCount++;
            }
            if (position + MINE_CONST - 1 < mineMapRange && mineArray[position + MINE_CONST - 1].equals("mine")) {
                mineSearchCount++;
            }
            if (position + MINE_CONST - 2 < mineMapRange && mineArray[position + MINE_CONST - 2].equals("mine")) {
                mineSearchCount++;
            }
        }
        return mineSearchCount;
    }

    public void mineEmptySearch(int position, int mineMapRange, final AdapterView<?> parent) {

        /**
         * 요구사항에 없는 부분이지만 정말 지뢰찾기 게임처럼 구현해보고 싶어서 추가하였습니다.
         * 재귀호출을 통해 이웃하는 빈 값을 찾아 모두 나타냅니다.
         * 왼쪽벽과 오른쪽벽에 대해서도 예외처리를 합니다.
         * **/

        if (position - MINE_CONST > -1 && (position % (MINE_CONST - 1)) != 0 && !mineArray[position - MINE_CONST].equals("mine")) {
            if (!mapClickList.contains(position - MINE_CONST)) {
                mineMapClick(position - MINE_CONST, parent);
            }
        }

        if (position - MINE_CONST + 1 > -1 && !mineArray[position - MINE_CONST + 1].equals("mine")) {
            if (!mapClickList.contains(position - MINE_CONST + 1)) {
                mineMapClick(position - MINE_CONST + 1, parent);
            }
        }

        if (position - MINE_CONST + 2 > -1 && ((position + 2) % (MINE_CONST - 1)) != 1 && !mineArray[position - MINE_CONST + 2].equals("mine")) {
            if (!mapClickList.contains(position - MINE_CONST + 2)) {
                mineMapClick(position - MINE_CONST + 2, parent);
            }
        }

        if (position - 1 > -1 && (position % (MINE_CONST - 1)) != 0 && !mineArray[position - 1].equals("mine")) {
            if (!mapClickList.contains(position - 1)) {
                mineMapClick(position - 1, parent);
            }
        }

        if (position + 1 < mineMapRange && ((position + 2) % (MINE_CONST - 1)) != 1 && !mineArray[position + 1].equals("mine")) {
            if (!mapClickList.contains(position + 1)) {
                mineMapClick(position + 1, parent);
            }
        }

        if (position + MINE_CONST < mineMapRange && ((position + 2) % (MINE_CONST - 1)) != 1 && !mineArray[position + MINE_CONST].equals("mine")) {
            if (!mapClickList.contains(position + MINE_CONST)) {
                mineMapClick(position + MINE_CONST, parent);
            }
        }

        if (position + MINE_CONST - 1 < mineMapRange && !mineArray[position + MINE_CONST - 1].equals("mine")) {
            if (!mapClickList.contains(position + MINE_CONST - 1)) {
                mineMapClick(position + MINE_CONST - 1, parent);
            }
        }

        if (position + MINE_CONST - 2 < mineMapRange && (position % (MINE_CONST - 1)) != 0 && !mineArray[position + MINE_CONST - 2].equals("mine")) {
            if (!mapClickList.contains(position + MINE_CONST - 2)) {
                mineMapClick(position + MINE_CONST - 2, parent);
            }
        }
    }

    public void gameOver(final AdapterView<?> parent) {

        /**
         * 지뢰를 다 찾거나 지뢰를 클릭하였을 때 호출합니다.
         * 지뢰가 아닌 부분을 깃발을 세울 경우 깃발이 아닌것으로 나타냈습니다.
         * **/

        vibrator.vibrate(new long[]{100, 200, 100, 150}, -1);
        timerInit();

        for (int x = 0; x < mineList.size(); x++) {
            if (mineFlagList.contains(mineList.get(x))) {
                mineFlagList.remove((Object) mineList.get(x));
            }
        }

        for (int y = 0; y < mineFlagList.size(); y++) {
            ImageView mineItem = (ImageView) parent.getChildAt(mineFlagList.get(y)).findViewById(R.id.mineBg);
            mineItem.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mineItem.setImageResource(R.drawable.un_flag);
        }

        for (int z = 0; z < mineList.size(); z++) {
            ImageView mineItem = (ImageView) parent.getChildAt(mineList.get(z)).findViewById(R.id.mineBg);
            mineItem.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mineItem.setImageResource(R.drawable.mine);
            mineMapGrid.setEnabled(false);
        }
    }

    public void reTry(View v) {

        /**
         * 모든 게임설정을 초기화합니다.
         * **/

        timerInit();
        mineMapGrid.setEnabled(true);
        mineList.clear();
        mapClickList.clear();
        mineFlagList.clear();
        actionCount = 0;
        mineInit(MINE_MAP_RANGE, MINE_COUNT);
    }

    public void timerInit() {

        /**
         * 타이머를 초기화합니다.
         * **/

        try {
            mTimeTask.cancel();
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void gameSetting(View v) {

        /**
         * 게임 설정값(가로 크기, 세로 크기, 지뢰 갯수)을 받습니다.
         * **/

        final AlertDialog.Builder settingDialog = new AlertDialog.Builder(this);
        settingDialog.setTitle("GAME SETTING");
        View dialogView;
        LayoutInflater mInflater = LayoutInflater.from(this);
        dialogView = mInflater.inflate(R.layout.dialog_setting, null);

        final EditText mapWidthEdit = (EditText) dialogView.findViewById(R.id.mapWidthEdit);
        final EditText mapHeightEdit = (EditText) dialogView.findViewById(R.id.mapHeightEdit);
        final EditText mineCountEdit = (EditText) dialogView.findViewById(R.id.mineCountEdit);

        mapWidthEdit.setText("" + mineMapGrid.getNumColumns());
        mapHeightEdit.setText("" + MINE_MAP_RANGE / mineMapGrid.getNumColumns());
        mineCountEdit.setText("" + MINE_COUNT);

        settingDialog.setView(dialogView);


        settingDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                if (Integer.parseInt(mapWidthEdit.getText().toString()) < 16 && Integer.parseInt(mapHeightEdit.getText().toString()) < 16) {
                    if (Integer.parseInt(mineCountEdit.getText().toString()) != 0) {
                        MINE_MAP_RANGE = Integer.parseInt(mapWidthEdit.getText().toString()) *
                                Integer.parseInt(mapHeightEdit.getText().toString());
                        MINE_COUNT = Integer.parseInt(mineCountEdit.getText().toString());
                        MINE_CONST = Integer.parseInt(mapWidthEdit.getText().toString()) + 1;
                        mineMapGrid.setNumColumns(Integer.parseInt(mapWidthEdit.getText().toString()));

                        dialog.dismiss();
                        reTry(null);
                    } else {
                        Toast.makeText(MainActivity.this, "지뢰크기는 최소 1개 입니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "가로/세로 크기는 최대 크기는 15칸 입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        settingDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        settingDialog.show();
    }
}
