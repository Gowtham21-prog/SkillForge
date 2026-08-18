import React, { useEffect, useState, useCallback } from 'react';
import { courseApi } from '../services/courseService';
import CourseCard from '../components/CourseCard';

const PAGE_SIZE = 12;

export default function Courses() {
  const [coursesPage, setCoursesPage] = useState({ content: [], totalPages: 0, totalElements: 0, number: 0 });
  const [loading, setLoading] = useState(true);
  const [categories, setCategories] = useState([]);

  const [keyword, setKeyword] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [category, setCategory] = useState('All');
  const [level, setLevel] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [sortBy, setSortBy] = useState('newest');
  const [page, setPage] = useState(0);

  useEffect(() => {
    courseApi.getCategories().then((res) => setCategories(res.data)).catch(() => {});
  }, []);

  const load = useCallback(() => {
    setLoading(true);
    const params = {
      keyword: keyword || undefined,
      category: category !== 'All' ? category : undefined,
      level: level || undefined,
      minPrice: minPrice || undefined,
      maxPrice: maxPrice || undefined,
      sortBy,
      page,
      size: PAGE_SIZE,
    };
    courseApi.getAll(params)
      .then((res) => setCoursesPage(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [keyword, category, level, minPrice, maxPrice, sortBy, page]);

  useEffect(() => { load(); }, [load]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setKeyword(keywordInput);
  };

  const resetFilters = () => {
    setKeywordInput('');
    setKeyword('');
    setCategory('All');
    setLevel('');
    setMinPrice('');
    setMaxPrice('');
    setSortBy('newest');
    setPage(0);
  };

  const { content: courses, totalPages, totalElements } = coursesPage;

  return (
    <div className="container page">
      <div className="section-head">
        <div>
          <span className="eyebrow">Catalog</span>
          <h2>Browse all courses</h2>
        </div>
        {!loading && <span style={{ fontSize: 13.5, color: 'var(--color-ink-soft)' }}>{totalElements} course{totalElements !== 1 ? 's' : ''} found</span>}
      </div>

      <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: 10, marginBottom: 18 }}>
        <input
          type="text"
          placeholder="Search by title or topic…"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          style={{ flex: 1, padding: '11px 14px', border: '1.5px solid var(--color-line)', borderRadius: 6, fontSize: 14.5 }}
        />
        <button type="submit" className="btn btn-dark">Search</button>
      </form>

      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 24, alignItems: 'center' }}>
        <select value={category} onChange={(e) => { setCategory(e.target.value); setPage(0); }} style={filterSelectStyle}>
          <option value="All">All categories</option>
          {categories.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>

        <select value={level} onChange={(e) => { setLevel(e.target.value); setPage(0); }} style={filterSelectStyle}>
          <option value="">Any level</option>
          <option value="BEGINNER">Beginner</option>
          <option value="INTERMEDIATE">Intermediate</option>
          <option value="ADVANCED">Advanced</option>
        </select>

        <input
          type="number"
          placeholder="Min $"
          value={minPrice}
          onChange={(e) => { setMinPrice(e.target.value); setPage(0); }}
          style={{ ...filterSelectStyle, width: 90 }}
        />
        <input
          type="number"
          placeholder="Max $"
          value={maxPrice}
          onChange={(e) => { setMaxPrice(e.target.value); setPage(0); }}
          style={{ ...filterSelectStyle, width: 90 }}
        />

        <select value={sortBy} onChange={(e) => { setSortBy(e.target.value); setPage(0); }} style={filterSelectStyle}>
          <option value="newest">Newest first</option>
          <option value="oldest">Oldest first</option>
          <option value="price">Price: low to high</option>
          <option value="title">Title: A–Z</option>
        </select>

        <button type="button" className="btn btn-sm" onClick={resetFilters} style={{ background: 'transparent', color: 'var(--color-ink-soft)', textDecoration: 'underline' }}>
          Clear filters
        </button>
      </div>

      {loading && <p>Loading courses…</p>}

      {!loading && courses.length === 0 && (
        <div className="empty-state">
          <h3>No courses found</h3>
          <p>Try a different search term, or widen your filters.</p>
        </div>
      )}

      <div className="course-grid">
        {courses.map((c) => (
          <CourseCard key={c.id} course={c} />
        ))}
      </div>

      {!loading && totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 36 }}>
          <button className="btn btn-sm btn-outline" style={{ color: 'var(--color-ink)', borderColor: 'var(--color-line)' }}
            onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>
            ← Prev
          </button>
          <span style={{ alignSelf: 'center', fontFamily: 'var(--font-mono)', fontSize: 13, color: 'var(--color-ink-soft)' }}>
            Page {page + 1} of {totalPages}
          </span>
          <button className="btn btn-sm btn-outline" style={{ color: 'var(--color-ink)', borderColor: 'var(--color-line)' }}
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>
            Next →
          </button>
        </div>
      )}
    </div>
  );
}

const filterSelectStyle = {
  padding: '9px 12px',
  border: '1.5px solid var(--color-line)',
  borderRadius: 6,
  fontSize: 13.5,
  background: 'white',
  color: 'var(--color-ink)',
};
