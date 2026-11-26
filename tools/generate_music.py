#!/usr/bin/env python3
"""ドパミン・モール用チップチューン風ループ BGM を生成するスクリプト。

Python 標準ライブラリのみで、矩形波・三角波・正弦波を合成して WAV を作る。
使い方: python3 tools/generate_music.py
出力先: android/assets/music/{title,dungeon,boss}.wav
（22050 Hz / モノラル / 16bit。ループ再生を前提に、曲の頭とお尻で音量が
 0 に近づくようエンベロープをかけ、継ぎ目でクリックノイズが出ないようにする）
"""
import math
import os
import random
import struct
import wave

SR = 22050
OUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                       "android", "assets", "music")


def midi_freq(midi):
    return 440.0 * (2.0 ** ((midi - 69) / 12.0))


def osc(kind, phase):
    """位相（ラジアン）から波形の振幅 -1.0〜1.0 を返す。"""
    if kind == "square":
        return 1.0 if math.sin(phase) >= 0 else -1.0
    if kind == "triangle":
        return (2.0 / math.pi) * math.asin(math.sin(phase))
    return math.sin(phase)


def render_note(buf, start_sec, length_sec, midi, kind, vol, attack_sec=0.005,
                release_sec=0.06):
    """1音をバッファに加算する。エンベロープ付き。"""
    start = int(start_sec * SR)
    n = int(length_sec * SR)
    freq = midi_freq(midi)
    phase = 0.0
    step = 2.0 * math.pi * freq / SR
    attack_n = max(1, int(attack_sec * SR))
    release_n = max(1, int(min(release_sec, length_sec * 0.4) * SR))
    for i in range(n):
        idx = start + i
        if idx >= len(buf):
            break
        # アタック：立ち上がり
        env = 1.0
        if i < attack_n:
            env = i / attack_n
        # リリース：終わりで 0 へ
        if i >= n - release_n:
            env *= (n - i) / release_n
        # 持続中はゆるやかに減衰
        env *= 1.0 - 0.25 * (i / n)
        buf[idx] += vol * env * osc(kind, phase)
        phase += step


def render_hat(buf, start_sec, length_sec, vol, rng):
    """ハイハット風の短いノイズ。"""
    start = int(start_sec * SR)
    n = int(length_sec * SR)
    for i in range(n):
        idx = start + i
        if idx >= len(buf):
            break
        env = 1.0 - (i / n)
        buf[idx] += vol * env * rng.uniform(-1.0, 1.0)


def render_track(bpm, bars, notes, hats=False):
    """notes: (midi, start_beat, length_beats, kind, vol, attack_sec, release_sec)
    のタプル列をレンダリングして float バッファを返す。"""
    beat_sec = 60.0 / bpm
    total_sec = bars * 4 * beat_sec
    buf = [0.0] * int(total_sec * SR)
    rng = random.Random(1234)
    for note in notes:
        midi, start_beat, length_beats, kind, vol = note[:5]
        attack = note[5] if len(note) > 5 else 0.005
        release = note[6] if len(note) > 6 else 0.06
        render_note(buf, start_beat * beat_sec, length_beats * beat_sec,
                    midi, kind, vol, attack, release)
    if hats:
        # 8分ごとのハイハット
        t = 0.5
        while t < bars * 4:
            render_hat(buf, t * beat_sec, 0.03, 0.05, rng)
            t += 1.0
    # 正規化（ピークを 0.85 に）
    peak = max(max(buf), -min(buf), 1e-6)
    scale = 0.85 / peak
    # 曲の頭とお尻に短いフェードをかけ、ループの継ぎ目を滑らかにする
    fade_n = int(0.03 * SR)
    for i in range(fade_n):
        f = i / fade_n
        buf[i] *= f
        buf[-1 - i] *= f
    return [s * scale for s in buf]


def write_wav(path, buf):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        frames = b"".join(struct.pack("<h", max(-32767, min(32767, int(s * 32767))))
                          for s in buf)
        w.writeframes(frames)
    print("wrote %s (%.1f sec, %d bytes)"
          % (path, len(buf) / SR, os.path.getsize(path)))


# ---------------------------------------------------------------------------
# (1) タイトル：明るくノリのいいチップチューン（132 BPM、8小節、Cメジャー）
#     進行：C - F - G - F - C - F - G - C
# ---------------------------------------------------------------------------
def third_below(midi):
    """Cメジャー／Aナチュラルマイナーの音階で3度下の音を返す（ハーモニー用）。
    黒鍵は半音下の白鍵に寄せる。"""
    scale = [0, 2, 4, 5, 7, 9, 11]
    pc = midi % 12
    octave = midi // 12
    if pc not in scale:
        pc -= 1
    idx = scale.index(pc)
    if idx >= 2:
        return octave * 12 + scale[idx - 2]
    return (octave - 1) * 12 + scale[idx - 2 + 7]


# ---------------------------------------------------------------------------
# (1) タイトル：明るくノリのいいチップチューン（132 BPM、8小節、Cメジャー）
#     進行：C - C - F - F - G - G - F - F
#     メロディ＋3度下のハーモニー、ウォーキングベース、要所のカウンターメロディ。
# ---------------------------------------------------------------------------
def title_notes():
    notes = []
    # ウォーキングベース（三角波）：8分でルート→5度→オクターブと歩く
    roots = [48, 48, 53, 53, 55, 55, 53, 53]  # C3 C3 F3 F3 G3 G3 F3 F3
    walk = [0, 7, 12, 7, 0, 5, 7, 10]
    for bar, root in enumerate(roots):
        for i, step in enumerate(walk):
            notes.append((root + step, bar * 4.0 + i * 0.5, 0.5, "triangle", 0.28))
    # メロディ（矩形波）：8分音符のリフ
    melody = [
        # 小節: (midi, 長さ(拍))
        [(76, 0.5), (79, 0.5), (81, 0.5), (79, 0.5), (76, 0.5), (79, 0.5), (84, 0.5), (79, 0.5)],
        [(81, 0.5), (84, 0.5), (81, 0.5), (77, 0.5), (81, 0.5), (79, 0.5), (81, 0.5), (84, 0.5)],
        [(83, 0.5), (86, 0.5), (83, 0.5), (79, 0.5), (83, 0.5), (81, 0.5), (79, 0.5), (74, 0.5)],
        [(81, 0.5), (84, 0.5), (81, 0.5), (77, 0.5), (76, 0.5), (74, 0.5), (72, 0.5), (74, 0.5)],
        [(76, 0.5), (79, 0.5), (81, 0.5), (79, 0.5), (76, 0.5), (79, 0.5), (84, 0.5), (79, 0.5)],
        [(81, 0.5), (84, 0.5), (86, 0.5), (84, 0.5), (81, 0.5), (79, 0.5), (81, 0.5), (77, 0.5)],
        [(83, 0.5), (86, 0.5), (83, 0.5), (79, 0.5), (81, 0.5), (83, 0.5), (86, 0.5), (83, 0.5)],
        [(84, 1.0), (79, 0.5), (76, 0.5), (74, 0.5), (72, 1.5)],
    ]
    for bar, seq in enumerate(melody):
        t = bar * 4.0
        for midi, length in seq:
            notes.append((midi, t, length, "square", 0.22))
            # 3度下のハーモニー（少し小さめ）
            notes.append((third_below(midi), t, length, "square", 0.11))
            t += length
    # カウンターメロディ（三角波・1オクターブ下）：偶数小節の1・3拍目で受け答え
    counter = {
        1: [(72, 0.0, 1.0), (67, 2.0, 1.0)],
        3: [(69, 0.0, 1.0), (65, 2.0, 1.0)],
        5: [(71, 0.0, 1.0), (67, 2.0, 1.0)],
        7: [(72, 0.0, 2.0)],
    }
    for bar, seq in counter.items():
        for midi, offset, length in seq:
            notes.append((midi, bar * 4.0 + offset, length, "triangle", 0.12))
    return notes


# ---------------------------------------------------------------------------
# (2) ダンジョン：控えめな環境音楽（72 BPM、8小節）
#     前半 Am - F - C - G、後半 Am - F - Dm - E（Eメジャーで少し緊張感を足す）。
#     正弦波のパッド＋三角波のアルペジオ＋まばらなリード旋律。
# ---------------------------------------------------------------------------
def dungeon_notes():
    notes = []
    chords = [
        [57, 60, 64],  # Am: A3 C4 E4
        [53, 57, 60],  # F:  F3 A3 C4
        [48, 52, 55],  # C:  C3 E3 G3
        [55, 59, 62],  # G:  G3 B3 D4
        [57, 60, 64],  # Am: A3 C4 E4
        [53, 57, 60],  # F:  F3 A3 C4
        [50, 53, 57],  # Dm: D3 F3 A3
        [52, 56, 59],  # E:  E3 G#3 B3
    ]
    # パッド（正弦波）：1小節まるごと和音を鳴らす
    for bar in range(8):
        chord = chords[bar]
        for midi in chord:
            notes.append((midi, bar * 4.0, 4.0, "sine", 0.10, 0.25, 0.4))
    # アルペジオ（三角波）：8分音符でコードトーンをゆっくり回す
    for bar in range(8):
        chord = chords[bar]
        tones = [chord[0], chord[1], chord[2], chord[1] + 12,
                 chord[2], chord[1] + 12, chord[1], chord[0] + 12]
        for i, midi in enumerate(tones):
            notes.append((midi, bar * 4.0 + i * 0.5, 0.5, "triangle", 0.10))
    # リード旋律（正弦波）：1小節に1〜2音の長い音で、コードトーンをなぞる
    lead = [
        [(76, 2.0), (74, 2.0)],  # E5 D5（Am）
        [(72, 4.0)],             # C5（F）
        [(67, 2.0), (69, 2.0)],  # G4 A4（C）
        [(71, 4.0)],             # B4（G）
        [(69, 2.0), (72, 2.0)],  # A4 C5（Am）
        [(69, 4.0)],             # A4（F）
        [(65, 2.0), (64, 2.0)],  # F4 E4（Dm）
        [(64, 4.0)],             # E4（E）
    ]
    for bar, seq in enumerate(lead):
        t = bar * 4.0
        for midi, length in seq:
            notes.append((midi, t, length, "sine", 0.09, 0.4, 0.6))
            t += length
    return notes


# ---------------------------------------------------------------------------
# (3) ボス：緊迫感のある速い曲（160 BPM、8小節、Aマイナー）
#     前半は駆け足の8分ベース＋速いリード（フラット2度 Bb で不穏さ）。
#     後半（5〜8小節）は Fマイナー寄りの展開＋ハーモニー＋低いドン（サイン波）で盛り上げる。
# ---------------------------------------------------------------------------
def boss_notes():
    notes = []
    # ベース（矩形波・低音）：8分で刻むリフ。前半 A、後半 F
    bass_riff_a = [45, 45, 48, 45, 40, 45, 43, 40]  # A2 A2 C3 A2 E2 A2 G2 E2
    bass_riff_b = [41, 41, 44, 41, 36, 41, 39, 36]  # F2 F2 Ab2 F2 C2 F2 Eb2 C2
    for bar in range(8):
        riff = bass_riff_a if bar < 4 else bass_riff_b
        for i, midi in enumerate(riff):
            notes.append((midi, bar * 4.0 + i * 0.5, 0.5, "square", 0.22))
    # 低いドン（正弦波）：各小節の1・3拍目に重い低音
    for bar in range(8):
        root = 33 if bar < 4 else 29  # A1 / F1
        notes.append((root, bar * 4.0, 0.3, "sine", 0.30, 0.005, 0.15))
        notes.append((root, bar * 4.0 + 2.0, 0.3, "sine", 0.30, 0.005, 0.15))
    # リード（矩形波）：16分っぽい速いフレーズ。後半は展開して高音で締める
    lead = [
        [(69, 0.5), (70, 0.5), (72, 0.5), (69, 0.5), (76, 0.5), (72, 0.5), (70, 0.5), (69, 0.5)],
        [(69, 0.5), (70, 0.5), (72, 0.5), (74, 0.5), (76, 0.5), (74, 0.5), (72, 0.5), (70, 0.5)],
        [(77, 0.5), (76, 0.5), (74, 0.5), (72, 0.5), (74, 0.5), (76, 0.5), (77, 0.5), (79, 0.5)],
        [(81, 0.5), (79, 0.5), (77, 0.5), (76, 0.5), (74, 0.5), (72, 0.5), (70, 0.5), (69, 0.5)],
        [(69, 0.5), (70, 0.5), (72, 0.5), (69, 0.5), (76, 0.5), (72, 0.5), (70, 0.5), (69, 0.5)],
        [(69, 0.5), (70, 0.5), (72, 0.5), (74, 0.5), (76, 0.5), (74, 0.5), (72, 0.5), (70, 0.5)],
        [(81, 0.5), (84, 0.5), (81, 0.5), (79, 0.5), (77, 0.5), (79, 0.5), (81, 0.5), (84, 0.5)],
        [(86, 1.0), (84, 0.5), (81, 0.5), (79, 1.0), (77, 1.0)],
    ]
    for bar, seq in enumerate(lead):
        t = bar * 4.0
        for midi, length in seq:
            notes.append((midi, t, length, "square", 0.16))
            # 3度下のハーモニー（少し小さめ）
            notes.append((third_below(midi), t, length, "square", 0.09))
            t += length
    return notes


def main():
    write_wav(os.path.join(OUT_DIR, "title.wav"),
              render_track(132, 8, title_notes(), hats=True))
    write_wav(os.path.join(OUT_DIR, "dungeon.wav"),
              render_track(72, 8, dungeon_notes()))
    write_wav(os.path.join(OUT_DIR, "boss.wav"),
              render_track(160, 8, boss_notes(), hats=True))


if __name__ == "__main__":
    main()
