import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { courseApi, fileApi } from '../services/courseService';

const emptyLecture = () => ({ title: '', videoUrl: '', content: '', durationMinutes: '', preview: false });

export default function CourseEditor() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [category, setCategory] = useState('');
  const [level, setLevel] = useState('BEGINNER');
  const [thumbnailUrl, setThumbnailUrl] = useState('');
  const [lectures, setLectures] = useState([emptyLecture()]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [uploadingThumb, setUploadingThumb] = useState(false);

  useEffect(() => {
    if (isEdit) {
      courseApi.getById(id).then((res) => {
        const c = res.data;
        setTitle(c.title);
        setDescription(c.description || '');
        setPrice(String(c.price));
        setCategory(c.category || '');
        setLevel(c.level || 'BEGINNER');
        setThumbnailUrl(c.thumbnailUrl || '');
        setLectures(
          c.lectures && c.lectures.length > 0
            ? c.lectures.map((l) => ({
                title: l.title,
                videoUrl: l.videoUrl || '',
                content: l.content || '',
                durationMinutes: l.durationMinutes || '',
                preview: l.preview || false,
              }))
            : [emptyLecture()]
        );
      }).catch(() => setError('Could not load course'));
    }
  }, [id, isEdit]);

  const updateLecture = (idx, field, value) => {
    setLectures((prev) => prev.map((l, i) => (i === idx ? { ...l, [field]: value } : l)));
  };

  const addLecture = () => setLectures((prev) => [...prev, emptyLecture()]);
  const removeLecture = (idx) => setLectures((prev) => prev.filter((_, i) => i !== idx));

  const handleThumbnailUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploadingThumb(true);
    try {
      const res = await fileApi.upload(file);
      setThumbnailUrl(res.data.url);
    } catch {
      setError('Thumbnail upload failed');
    } finally {
      setUploadingThumb(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!title.trim() || price === '') {
      setError('Title and price are required');
      return;
    }

    const payload = {
      title,
      description,
      price: parseFloat(price),
      category,
      level,
      thumbnailUrl,
      lectures: lectures
        .filter((l) => l.title.trim())
        .map((l, idx) => ({
          title: l.title,
          videoUrl: l.videoUrl,
          content: l.content,
          durationMinutes: l.durationMinutes ? parseInt(l.durationMinutes, 10) : null,
          orderIndex: idx,
          preview: l.preview,
        })),
    };

    setSaving(true);
    try {
      if (isEdit) {
        await courseApi.update(id, payload);
        navigate('/instructor');
      } else {
        const res = await courseApi.create(payload);
        navigate(`/courses/${res.data.id}`);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Could not save course');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="container page">
      <div className="section-head">
        <div>
          <span className="eyebrow">{isEdit ? 'Edit course' : 'New course'}</span>
          <h2>{isEdit ? 'Update your course' : 'List a new course'}</h2>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-card" style={{ maxWidth: 720, marginBottom: 24 }}>
          <div className="form-group">
            <label>Course title</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea rows={4} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="form-row-2">
            <div className="form-group">
              <label>Price (USD)</label>
              <input type="number" step="0.01" min="0" value={price} onChange={(e) => setPrice(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Level</label>
              <select value={level} onChange={(e) => setLevel(e.target.value)}>
                <option value="BEGINNER">Beginner</option>
                <option value="INTERMEDIATE">Intermediate</option>
                <option value="ADVANCED">Advanced</option>
              </select>
            </div>
          </div>
          <div className="form-group">
            <label>Category</label>
            <input value={category} onChange={(e) => setCategory(e.target.value)} placeholder="e.g. Web Development, Design, Marketing" />
          </div>
          <div className="form-group">
            <label>Thumbnail</label>
            <input type="file" accept="image/*" onChange={handleThumbnailUpload} />
            {uploadingThumb && <div className="form-hint">Uploading…</div>}
            {thumbnailUrl && <div className="form-hint">Uploaded ✓</div>}
          </div>
        </div>

        <div style={{ maxWidth: 720 }}>
          <div className="section-head" style={{ marginBottom: 16 }}>
            <h3 style={{ fontSize: 20 }}>Lectures</h3>
            <button type="button" className="btn btn-sm btn-outline" style={{ color: 'var(--color-ink)', borderColor: 'var(--color-line)' }} onClick={addLecture}>
              + Add lecture
            </button>
          </div>

          {lectures.map((lec, idx) => (
            <div className="lecture-editor-row" key={idx}>
              <div className="lecture-editor-head">
                <strong style={{ fontSize: 13.5 }}>Lecture {idx + 1}</strong>
                {lectures.length > 1 && (
                  <button type="button" className="btn btn-sm btn-danger" onClick={() => removeLecture(idx)}>Remove</button>
                )}
              </div>
              <div className="form-group">
                <label>Title</label>
                <input value={lec.title} onChange={(e) => updateLecture(idx, 'title', e.target.value)} />
              </div>
              <div className="form-row-2">
                <div className="form-group">
                  <label>Video URL</label>
                  <input value={lec.videoUrl} onChange={(e) => updateLecture(idx, 'videoUrl', e.target.value)} placeholder="https://…" />
                </div>
                <div className="form-group">
                  <label>Duration (minutes)</label>
                  <input type="number" value={lec.durationMinutes} onChange={(e) => updateLecture(idx, 'durationMinutes', e.target.value)} />
                </div>
              </div>
              <div className="form-group">
                <label>Notes / content</label>
                <textarea rows={2} value={lec.content} onChange={(e) => updateLecture(idx, 'content', e.target.value)} />
              </div>
              <label style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}>
                <input type="checkbox" checked={lec.preview} onChange={(e) => updateLecture(idx, 'preview', e.target.checked)} />
                Free preview
              </label>
            </div>
          ))}
        </div>

        <button type="submit" className="btn btn-primary" style={{ marginTop: 8 }} disabled={saving}>
          {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Publish course'}
        </button>
      </form>
    </div>
  );
}
